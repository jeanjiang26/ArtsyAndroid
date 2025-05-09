package com.jeannie.artsyfinal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeannie.artsyfinal.models.Artist
import com.jeannie.artsyfinal.models.ArtistDetail
import com.jeannie.artsyfinal.models.Artwork
import com.jeannie.artsyfinal.models.Category
import com.jeannie.artsyfinal.network.ApiService
import com.jeannie.artsyfinal.network.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Result<out T> {
    object Loading : Result<Nothing>()
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

class ArtistDetailViewModel : ViewModel() {
    private val apiService = RetrofitClient.statelessClient.create(ApiService::class.java)

    private val _artistDetails = MutableStateFlow<ArtistDetail?>(null)
    val artistDetails = _artistDetails.asStateFlow()

    private val _artworks = MutableStateFlow<List<Artwork>>(emptyList())
    val artworks = _artworks.asStateFlow()

    // Add state for similar artists
    private val _similarArtists = MutableStateFlow<List<Artist>>(emptyList())
    val similarArtists = _similarArtists.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadArtistDetails(artistId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d("ArtistDetailViewModel", "Loading details for artist: $artistId")

                val detailsDeferred = async { apiService.getArtistDetail(artistId) }
                val artworksDeferred = async {
                    try {
                        apiService.getArtistArtworks(artistId).artworks
                    } catch (e: Exception) {
                        Log.w("ArtistDetailViewModel", "No artworks found or failed to load artworks", e)
                        emptyList()
                    }
                }

                _artistDetails.value = detailsDeferred.await()
                _artworks.value = artworksDeferred.await()

            } catch (e: Exception) {
                Log.e("ArtistDetailViewModel", "Error loading artist details", e)
                _error.value = "Failed to load artist details: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadArtistArtworks(artistId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getArtistArtworks(artistId)
                _artworks.value = response.artworks
            } catch (e: Exception) {
                Log.e("ArtistDetailViewModel", "Error loading artworks", e)
            }
        }
    }

    // Add function to load similar artists
//    fun loadSimilarArtists(artistId: String) {
//        viewModelScope.launch {
//            try {
//                Log.d("ArtistDetailViewModel", "Loading similar artists for: $artistId")
//                val response = apiService.getSimilarArtists(artistId)
//                _similarArtists.value = response.data
//                Log.d("ArtistDetailViewModel", "Loaded ${response.data.size} similar artists")
//            } catch (e: Exception) {
//                Log.e("ArtistDetailViewModel", "Error loading similar artists", e)
//                _similarArtists.value = emptyList()
//            }
//        }
//    }

    // Update the loadSimilarArtists function in ArtistDetailViewModel:

    fun loadSimilarArtists(artistId: String) {
        viewModelScope.launch {
            try {
                Log.d("ArtistDetailViewModel", "Loading similar artists for: $artistId")

                // Call the API endpoint
                val response = apiService.getSimilarArtists(artistId)

                // Since response is now directly a List<Artist>, we can assign it directly
                _similarArtists.value = response

                Log.d("ArtistDetailViewModel", "Loaded ${response.size} similar artists")
            } catch (e: Exception) {
                Log.e("ArtistDetailViewModel", "Error loading similar artists", e)
                _similarArtists.value = emptyList()
            }
        }
    }

    fun getArtworkCategories(artworkId: String): Flow<Result<List<Category>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getArtworkCategories(artworkId)
            emit(Result.Success(response.genes))
        } catch (e: Exception) {
            Log.e("ArtistDetailViewModel", "Error loading categories", e)
            emit(Result.Error(e.message ?: "Failed to load categories"))
        }
    }
}