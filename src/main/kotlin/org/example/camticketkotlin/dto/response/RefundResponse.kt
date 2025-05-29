package org.example.camticketkotlin.dto.response

import org.example.camticketkotlin.domain.ReservationStatus
import java.time.LocalDateTime

data class RefundResponse(
    val reservationId: Long,
    val performanceTitle: String,
    val performanceDate: LocalDateTime,
    val ticketOptionName: String,
    val totalPrice: Int,
    val status: ReservationStatus,
    val requestDate: LocalDateTime
)