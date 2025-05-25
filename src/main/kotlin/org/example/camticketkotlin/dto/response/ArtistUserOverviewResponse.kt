package org.example.camticketkotlin.dto.response

data class ArtistUserOverviewResponse(
    val userId: Long,
    val nickName: String,
    val profileImageUrl: String?
)
