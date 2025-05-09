// models/ApiModels.kt
package com.jeannie.artsyfinal.models

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    val artists: List<Artist>?
)

data class FavoriteArtistEntry(
    val artist: Artist,
    val artistId: String,
    val timestamp: Long = System.currentTimeMillis()
) {

    constructor(artist: Artist, timestamp: Long = System.currentTimeMillis()) : this(
        artist = artist,
        artistId = artist.id,
        timestamp = timestamp
    )
}

data class Artist(
    @SerializedName("artistId") val id: String,
    val name: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    val birthday: String?,
    val nationality: String?,
    val timestamp: String? = null
)


data class ArtistLinks(
    val self: Link?,
    val permalink: Link?,
    val thumbnail: Link?
)

data class Link(
    val href: String
)


data class Artwork(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val date: String?
)

data class Category(
    val name: String,
    val imageUrl: String?
)
data class ArtistDetailsResponse(
    val data: ArtistDetail
)

data class ArtistDetail(
    val artistId: String,
    val name: String,
    val birthday: String?,
    val deathday: String?,
    val nationality: String?,
    val biography: String?,
    val imageUrl: String?
)

data class ArtworksResponse(
    val artworks: List<Artwork>
)

data class ArtworkCategoriesResponse(
    val genes: List<Category>
)

