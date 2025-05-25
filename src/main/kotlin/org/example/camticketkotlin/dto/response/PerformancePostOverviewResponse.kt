package org.example.camticketkotlin.dto.response

import java.time.LocalDateTime

data class PerformancePostOverviewResponse(
    val postId: Long,
    val title: String,
    val profileImageUrl: String,
    val reservationStartAt: LocalDateTime,
    val reservationEndAt: LocalDateTime,
    val firstScheduleStartTime: LocalDateTime,
    val location: String,
    val userId: Long
)
