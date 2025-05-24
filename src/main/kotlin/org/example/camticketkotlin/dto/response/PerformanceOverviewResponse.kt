package org.example.camticketkotlin.dto.response

data class PerformanceOverviewResponse(
    val postId: Long,
    val profileImageUrl: String,
    val lastScheduleTime: String
)
