package org.example.camticketkotlin.dto.response

import java.time.LocalDateTime

data class PerformanceScheduleResponse(
    val scheduleId: Long,
    val startTime: LocalDateTime,
    val availableSeats: Int,      // 남은 좌석 수
    val totalSeats: Int,          // 전체 좌석 수
    val isBookingAvailable: Boolean  // 예매 가능 여부
)