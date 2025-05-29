package org.example.camticketkotlin.dto.response

import org.example.camticketkotlin.domain.ReservationStatus
import java.time.LocalDateTime

data class ReservationResponse(
    val reservationId: Long,
    val performanceTitle: String,
    val performanceDate: LocalDateTime,
    val ticketOptionName: String,
    val ticketPrice: Int,
    val count: Int,
    val totalPrice: Int,
    val status: ReservationStatus,
    val selectedSeats: List<String>,  // 선택한 좌석 코드들
    val regDate: LocalDateTime
)