package org.example.camticketkotlin.dto.request

import org.example.camticketkotlin.domain.enums.PerformanceCategory
import org.example.camticketkotlin.domain.enums.PerformanceLocation
import org.example.camticketkotlin.domain.enums.TicketType
import java.time.LocalDateTime

data class PerformancePostUpdateRequest(
    val id: Long,
    val title: String,
    val category: PerformanceCategory,
    val location: PerformanceLocation,
    val ticketType: TicketType,
    val maxTicketsPerUser: Int,
    val backAccount: String,
    val reservationStartAt: LocalDateTime,
    val reservationEndAt: LocalDateTime,
    val timeNotice: String,
    val priceNotice: String,
    val reservationNotice: String,
    val schedules: List<ScheduleRequest>,
    val seatUnavailableCodesPerSchedule: List<SeatUnavailableScheduleRequest>,
    val ticketOptions: List<TicketOptionRequest>
)

