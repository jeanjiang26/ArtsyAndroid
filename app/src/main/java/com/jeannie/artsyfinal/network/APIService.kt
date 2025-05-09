// network/ApiService.kt
package com.jeannie.artsyfinal.network

import com.jeannie.artsyfinal.models.*
import retrofit2.Response
import retrofit2.http.*

data class RegisterRequest(val fullname: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)

data class UserInfo(
    val fullname: String,
    val email: String,
    val profileImageUrl: String
)

data class UserResponse(
    val message: String,
    val user: UserInfo?
)

data class StatusResponse(
    val isAuthenticated: Boolean,
    val user: UserInfo?
)

data class MessageResponse(
    val message: String
)



typealias SimilarArtistsResponse = List<Artist>


data class User(
    val fullname: String,
    val email: String,
    val profileImageUrl: String
)

data class FavoritesResponse(
    val favorites: List<Artist>
)

interface ApiService {
    companion object {
        const val BASE_URL = "https://csci571-jeannie-project.uw.r.appspot.com/"
    }

    //Artist Search
    @GET("api/search")
    suspend fun searchArtists(@Query("term") query: String): SearchResponse

    //Artist Details
    @GET("api/artist/{id}")
    suspend fun getArtistDetail(@Path("id") id: String): ArtistDetail

    //Artist Artworks
    @GET("api/artwork/{artistId}")
    suspend fun getArtistArtworks(@Path("artistId") artistId: String): ArtworksResponse

    //Artwork Categories
    @GET("api/artwork/genes/{artworkId}")
    suspend fun getArtworkCategories(@Path("artworkId") artworkId: String): ArtworkCategoriesResponse

    //User Registration
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    //User Login
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): UserResponse

    //Logout
    @POST("api/auth/logout")
    suspend fun logout(): MessageResponse

    //Check Auth Status
    @GET("api/auth/status")
    suspend fun checkAuthStatus(): StatusResponse

    //Delete Account
    @DELETE("api/auth/delete")
    suspend fun deleteAccount(): MessageResponse

    //Get favorites
    @GET("api/favorites")
    suspend fun getFavorites(): FavoritesResponse

    //Add favorite
    @POST("api/favorites")
    suspend fun addFavorite(@Body body: Map<String, String>): Response<Void>

    //Remove favorite
    @DELETE("api/favorites/{artistId}")
    suspend fun removeFavorite(@Path("artistId") artistId: String): Response<Void>

    //http://localhost:5000/api/artist/similar/4d8b92774eb68a1b2c000134
    //Similar Artists
    @GET("api/artist/similar/{artistId}")
    suspend fun getSimilarArtists(@Path("artistId") artistId: String): SimilarArtistsResponse


}
