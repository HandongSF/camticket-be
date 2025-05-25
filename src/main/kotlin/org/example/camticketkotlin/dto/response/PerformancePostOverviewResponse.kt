package org.example.camticketkotlin.dto.response

import org.example.camticketkotlin.domain.enums.PerformanceCategory
import java.time.LocalDateTime

data class PerformancePostOverviewResponse(
    val postId: Long,
    val title: String,
    val profileImageUrl: String,
    val reservationStartAt: LocalDateTime,
    val reservationEndAt: LocalDateTime,
    val firstScheduleStartTime: LocalDateTime,
    val location: String,
    val userId: Long,
    val category: PerformanceCategory,
    val isClosed: Boolean

)
