package com.jeannie.artsyfinal.viewmodel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jeannie.artsyfinal.models.Artist
import com.jeannie.artsyfinal.models.FavoriteArtistEntry
import com.jeannie.artsyfinal.network.ApiService
import com.jeannie.artsyfinal.network.RetrofitClient
import com.jeannie.artsyfinal.network.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.os.Bundle

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val uid: String = "", val avatarUrl: String? = null) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AuthViewModel"
    private val apiService = RetrofitClient.getClientWithCookies(application).create(ApiService::class.java)

    // SharedPreferences for persisting timestamps
    private val sharedPreferences = application.getSharedPreferences(
        "ArtistFavorites", Context.MODE_PRIVATE
    )

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Update to use FavoriteArtistEntry
    private val _favoriteEntries = MutableStateFlow<List<FavoriteArtistEntry>>(emptyList())
    val favoriteEntries: StateFlow<List<FavoriteArtistEntry>> = _favoriteEntries.asStateFlow()

    // Keep this for compatibility with existing code
    private val _favorites = MutableStateFlow<List<Artist>>(emptyList())
    val favorites: StateFlow<List<Artist>> = _favorites.asStateFlow()

    // Track current user session
    private var currentSessionId: String? = null

    // Track current user email for debugging
    private var currentUserEmail: String? = null

    init {
        checkAuthStatus()
    }

    fun setNewUser(user: UserInfo) {
        Log.d(TAG, "===== SETTING NEW USER =====")
        Log.d(TAG, "User email: ${user.email}")
        Log.d(TAG, "User avatar: ${user.profileImageUrl}")

        // Store user email for debugging
        currentUserEmail = user.email

        // Generate a completely new session ID
        val newSessionId = "session_${System.currentTimeMillis()}"
        Log.d(TAG, "Generated new session ID: $newSessionId")

        // Store the new session ID
        currentSessionId = newSessionId

        // Clear old favorites data
        _favoriteEntries.value = emptyList()
        _favorites.value = emptyList()

        // Update auth state with new user
        _authState.value = AuthState.Authenticated(
            uid = newSessionId,
            avatarUrl = user.profileImageUrl
        )

        // Load favorites for the new user
        loadFavorites()

        Log.d(TAG, "New user set successfully")
    }

    fun checkAuthStatus() {
        Log.d(TAG, "===== CHECKING AUTH STATUS =====")

        viewModelScope.launch {
            try {
                val response = apiService.checkAuthStatus()
                Log.d(TAG, "Auth response: authenticated=${response.isAuthenticated}")

                if (response.isAuthenticated && response.user != null) {
                    Log.d(TAG, "User authenticated: ${response.user.email}")
                    Log.d(TAG, "User avatar: ${response.user.profileImageUrl}")

                    // Update current user email for debugging
                    currentUserEmail = response.user.email

                    // Check if this is a new user session or we're refreshing existing
                    if (currentSessionId == null) {
                        val newSessionId = "session_${System.currentTimeMillis()}"
                        Log.d(TAG, "Generated new session ID: $newSessionId")
                        currentSessionId = newSessionId

                        // Clear all previous data first
                        _favoriteEntries.value = emptyList()
                        _favorites.value = emptyList()
                    }

                    _authState.value = AuthState.Authenticated(
                        uid = currentSessionId ?: "",
                        avatarUrl = response.user.profileImageUrl
                    )

                    // Load favorites for this user
                    loadFavorites()

                    Log.d(TAG, "Authentication check completed for: $currentUserEmail")
                } else {
                    Log.d(TAG, "User not authenticated or no user data")
                    clearAllUserData()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking auth status", e)
                clearAllUserData()
            }
        }
    }

    // Inside AuthViewModel, add this function
    // Inside AuthViewModel, replace the debugAuthState() function with this fixed version
    fun debugAuthState() {
        // Use getApplication() as context instead of undefined context variable
        RetrofitClient.dumpCookies(getApplication())

        // Log the current authState
        Log.d(TAG, "🔐 Current Auth State: ${_authState.value}")

        // If authenticated, log user info
        if (_authState.value is AuthState.Authenticated) {
            val authUser = (_authState.value as AuthState.Authenticated)
            Log.d(TAG, "🔐 Authenticated as: $currentUserEmail (Session: ${authUser.uid})")
        }
    }

    fun loadFavorites() {
        //Log.d(TAG, "Loading favorites for user: ${getCurrentUserEmail()} (session: ${getCurrentSessionId()})")
        debugAuthState()
        viewModelScope.launch {
            try {
                // Make sure we have a session ID
                val sessionId = currentSessionId ?: return@launch

                Log.d(TAG, "Loading favorites for user: $currentUserEmail (session: $sessionId)")

                // Clear current lists first to ensure UI is refreshed properly
                _favoriteEntries.value = emptyList()
                _favorites.value = emptyList()

                // API call to load favorites
                val response = apiService.getFavorites()
                val artistsFromApi = response.favorites

                Log.d(TAG, "Received ${artistsFromApi.size} favorites from API")

                // Create entries with server timestamps when available
                val entries = artistsFromApi.map { artist ->
                    // Parse the timestamp from the artist if it exists
                    val serverTimestamp = if (!artist.timestamp.isNullOrEmpty()) {
                        try {
                            // Parse ISO-8601 timestamp format "2025-05-05T05:43:17.177Z"
                            val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                            isoDateFormat.timeZone = TimeZone.getTimeZone("UTC")
                            isoDateFormat.parse(artist.timestamp)?.time
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing timestamp: ${e.message}")
                            null
                        }
                    } else null

                    // Use the server timestamp if available, otherwise fall back to local persistence
                    val timestamp = serverTimestamp ?: getPersistedTimestamp(sessionId, artist.id)
                    FavoriteArtistEntry(artist, artist.id, timestamp)
                }

                // Update both streams
                _favoriteEntries.value = entries
                _favorites.value = artistsFromApi

                Log.d(TAG, "Updated favorites for user: $currentUserEmail (count: ${entries.size})")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading favorites: ${e.message}")
                // Only clear lists if they're not already empty
                if (_favoriteEntries.value.isNotEmpty()) {
                    _favoriteEntries.value = emptyList()
                }
                if (_favorites.value.isNotEmpty()) {
                    _favorites.value = emptyList()
                }
            }
        }
    }

    // Get persisted timestamp for an artist with session-specific key
    private fun getPersistedTimestamp(sessionId: String, artistId: String): Long {
        // Create a session-specific key for the timestamp
        val timestampKey = "timestamp_${sessionId}_$artistId"
        val timestamp = sharedPreferences.getLong(timestampKey, 0)
        return if (timestamp > 0) {
            timestamp
        } else {
            // If no timestamp exists, create and save one
            val newTimestamp = System.currentTimeMillis()
            saveTimestamp(sessionId, artistId, newTimestamp)
            newTimestamp
        }
    }

    // Save timestamp to SharedPreferences with session-specific key
    private fun saveTimestamp(sessionId: String, artistId: String, timestamp: Long) {
        val timestampKey = "timestamp_${sessionId}_$artistId"
        sharedPreferences.edit().putLong(timestampKey, timestamp).apply()
    }

    suspend fun addFavorite(artist: Artist) {
        try {
            // Make sure we have a session ID
            val sessionId = currentSessionId ?: throw IllegalStateException("User not authenticated")

            Log.d(TAG, "Adding favorite for user $currentUserEmail: ${artist.name} (${artist.id})")

            // Create request body with artistId
            val requestBody = mapOf("artistId" to artist.id)

            // Set and persist the timestamp when the artist is added to favorites
            val timestamp = System.currentTimeMillis()
            saveTimestamp(sessionId, artist.id, timestamp)

            // API call
            apiService.addFavorite(requestBody)

            // Create favorite entry with the new timestamp
            val favoriteEntry = FavoriteArtistEntry(artist, artist.id, timestamp)

            // Update local favorites lists
            val updatedEntries = _favoriteEntries.value.toMutableList()
            if (!updatedEntries.any { it.artistId == artist.id }) {
                updatedEntries.add(0, favoriteEntry) // Add to beginning of list
            }
            _favoriteEntries.value = updatedEntries

            // Keep original list in sync
            val updatedArtists = _favorites.value.toMutableList()
            if (!updatedArtists.any { it.id == artist.id }) {
                updatedArtists.add(0, artist)
            }
            _favorites.value = updatedArtists

            Log.d(TAG, "Successfully added favorite for user $currentUserEmail")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding favorite: ${e.message}", e)
            throw e
        }
    }

    suspend fun removeFavorite(artistId: String) {
        try {
            // Make sure we have a session ID
            val sessionId = currentSessionId ?: throw IllegalStateException("User not authenticated")

            Log.d(TAG, "Removing favorite for user $currentUserEmail: $artistId")
            apiService.removeFavorite(artistId)

            // Remove timestamp from SharedPreferences
            val timestampKey = "timestamp_${sessionId}_$artistId"
            sharedPreferences.edit().remove(timestampKey).apply()

            // Update local favorites lists
            val updatedEntries = _favoriteEntries.value.filter { it.artistId != artistId }
            _favoriteEntries.value = updatedEntries

            val updatedArtists = _favorites.value.filter { it.id != artistId }
            _favorites.value = updatedArtists

            Log.d(TAG, "Successfully removed favorite for user $currentUserEmail")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing favorite: ${e.message}", e)
            throw e
        }
    }

    fun logout() {
        Log.d(TAG, "===== LOGOUT INITIATED for user: $currentUserEmail =====")

        viewModelScope.launch {
            try {
                // Call the logout API
                apiService.logout()

                // Clear cookies BEFORE clearing user data
                RetrofitClient.clearCookies(getApplication())

                // Clear all user data
                clearAllUserData()

                Log.d(TAG, "Logout completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
                // Even if API call fails, still clear local state

                RetrofitClient.clearCookies(getApplication())
                clearAllUserData()
            }
        }
    }

    private fun clearAllUserData() {
        Log.d(TAG, "Clearing all user data for: $currentUserEmail")

        // Clear favorites in memory
        _favoriteEntries.value = emptyList()
        _favorites.value = emptyList()

        // If we have a session ID, clear its timestamps
        currentSessionId?.let { sessionId ->
            clearSessionTimestamps(sessionId)
        }

        // Clear session ID and user email
        currentSessionId = null
        currentUserEmail = null

        // Update the auth state to Unauthenticated
        _authState.value = AuthState.Unauthenticated

        Log.d(TAG, "All user data cleared")
    }

    fun deleteAccount() {
        Log.d(TAG, "===== DELETE ACCOUNT INITIATED for user: $currentUserEmail =====")

        viewModelScope.launch {
            try {
                // Call the delete account API
                apiService.deleteAccount()

                // Clear cookies BEFORE clearing user data
                RetrofitClient.clearCookies(getApplication())

                // Clear all user data
                clearAllUserData()

                Log.d(TAG, "Account deleted successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting account", e)
                // Even if API call fails, still clear local state
                RetrofitClient.clearCookies(getApplication())
                clearAllUserData()
            }
        }
    }

    private fun clearSessionTimestamps(sessionId: String) {
        // Only clear timestamps with the specific session's prefix
        val sessionPrefix = "timestamp_${sessionId}_"
        val editor = sharedPreferences.edit()

        sharedPreferences.all.keys.forEach { key ->
            if (key.startsWith(sessionPrefix)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    // Helper to check if an artist is a favorite
    fun isArtistFavorite(artistId: String): Boolean {
        return _favoriteEntries.value.any { it.artistId == artistId }
    }

    // Update timestamp when viewing an artist
    fun updateArtistTimestamp(artistId: String) {
        viewModelScope.launch {
            try {
                // Make sure we have a session ID
                val sessionId = currentSessionId ?: return@launch

                val timestamp = System.currentTimeMillis()
                saveTimestamp(sessionId, artistId, timestamp)

                // Update the entry in memory
                val updatedEntries = _favoriteEntries.value.toMutableList()
                val index = updatedEntries.indexOfFirst { it.artistId == artistId }

                if (index != -1) {
                    val entry = updatedEntries[index]
                    updatedEntries[index] = FavoriteArtistEntry(
                        artist = entry.artist,
                        artistId = entry.artistId,
                        timestamp = timestamp
                    )
                    _favoriteEntries.value = updatedEntries

                    Log.d(TAG, "Updated timestamp for artist $artistId for user $currentUserEmail")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating artist timestamp: ${e.message}")
            }
        }
    }

    /**
     * Register activity lifecycle callbacks to refresh data when app comes back to foreground
     */
    fun registerActivityLifecycleCallbacks(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: Bundle?) {
                Log.d(TAG, "Activity created: ${activity.javaClass.simpleName}")
            }

            override fun onActivityStarted(activity: android.app.Activity) {
                Log.d(TAG, "Activity started: ${activity.javaClass.simpleName}")
                // Refresh authenticated state when main activity starts
                if (activity.javaClass.simpleName == "MainActivity" && _authState.value is AuthState.Authenticated) {
                    Log.d(TAG, "MainActivity started - refreshing favorites automatically")
                    loadFavorites() // Use loadFavorites instead of forceRefreshFavorites
                }
            }

            override fun onActivityResumed(activity: android.app.Activity) {
                Log.d(TAG, "Activity resumed: ${activity.javaClass.simpleName}")
            }

            // Implement other required callbacks
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }
}