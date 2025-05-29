package org.example.camticketkotlin.dto.response

import org.example.camticketkotlin.domain.ReservationStatus
import java.time.LocalDateTime

data class ReservationManagementResponse(
    val reservationId: Long,
    val performanceTitle: String,
    val performanceDate: LocalDateTime,
    val userName: String,
    val userEmail: String,
    val ticketOptionName: String,
    val ticketPrice: Int,
    val count: Int,
    val totalPrice: Int,
    val status: ReservationStatus,
    val selectedSeats: List<String>,
    val regDate: LocalDateTime
)