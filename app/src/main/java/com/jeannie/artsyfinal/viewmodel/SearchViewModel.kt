package com.jeannie.artsyfinal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeannie.artsyfinal.models.Artist
import com.jeannie.artsyfinal.network.ApiService
import com.jeannie.artsyfinal.network.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.statelessClient.create(ApiService::class.java)
    private val authenticatedApiService = RetrofitClient.getClientWithCookies(application).create(ApiService::class.java)

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Artist>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites = _favorites.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadFavorites()
    }

    // Add this new method to update the query state without triggering a search
    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun loadFavorites() {
        viewModelScope.launch {
            try {
                val response = authenticatedApiService.getFavorites()
                _favorites.value = response.favorites.map { it.id }.toSet()
                Log.d("SearchViewModel", "Loaded favorites: ${_favorites.value}")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error loading favorites", e)
            }
        }
    }

    fun searchArtists(query: String) {
        // Don't update the query here - it's now handled by updateQuery
        // This prevents trimming the spaces during typing
        searchJob?.cancel()

        // Only trim when actually performing the search, not when storing the query
        val trimmedQuery = query.trim()

        // Return early for empty or too short queries
        if (trimmedQuery.isEmpty()) {
            Log.d("SearchViewModel", "Empty query, skipping search")
            _searchResults.value = emptyList()
            _isLoading.value = false
            _error.value = null
            return
        }

        if (trimmedQuery.length < 3) {
            Log.d("SearchViewModel", "Query too short: '$trimmedQuery'")
            _searchResults.value = emptyList()
            _isLoading.value = false
            _error.value = null
            return
        }

        searchJob = viewModelScope.launch {
            try {
                delay(500)  // debounce

                val endpointUrl = "${ApiService.BASE_URL}api/search?term=${trimmedQuery}"
                Log.d("SearchViewModel", "API Endpoint: $endpointUrl")

                _isLoading.value = true
                _error.value = null

                // Only use trimmed query for actual API request
                val response = apiService.searchArtists(trimmedQuery)
                val artists = response.artists ?: emptyList()

                Log.d("SearchViewModel", "Artists found: ${artists.size}")
                _searchResults.value = artists
                _error.value = null

            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d("SearchViewModel", "Search cancelled for query: '$trimmedQuery'")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching for '$trimmedQuery'", e)
                _searchResults.value = emptyList()
                _error.value = "Failed to search: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun addFavorite(artist: Artist) {
        try {
            Log.d("SearchViewModel", "Adding favorite: ${artist.id}")
            val requestBody = mapOf("artistId" to artist.id)
            val response = authenticatedApiService.addFavorite(requestBody)

            if (response.isSuccessful) {
                _favorites.value = _favorites.value + artist.id
                Log.d("SearchViewModel", "Added favorite: ${artist.id}")
            } else {
                throw Exception("Add favorite failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Error adding favorite", e)
            throw e
        }
    }

    suspend fun removeFavorite(artist: Artist) {
        try {
            Log.d("SearchViewModel", "Removing favorite: ${artist.id}")
            val response = authenticatedApiService.removeFavorite(artist.id)

            if (response.isSuccessful) {
                _favorites.value = _favorites.value - artist.id
                Log.d("SearchViewModel", "Removed favorite: ${artist.id}")
            } else {
                throw Exception("Remove favorite failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Error removing favorite", e)
            throw e
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}