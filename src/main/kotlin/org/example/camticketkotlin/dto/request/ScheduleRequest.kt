package org.example.camticketkotlin.dto.request

import org.example.camticketkotlin.domain.PerformancePost
import org.example.camticketkotlin.domain.PerformanceSchedule
import java.time.LocalDateTime

data class ScheduleRequest(
    val scheduleIndex: Int,
    val startTime: LocalDateTime
) {
    fun toEntity(post: PerformancePost): PerformanceSchedule {
        return PerformanceSchedule(
            startTime = this.startTime,
            performancePost = post
        )
    }
}
