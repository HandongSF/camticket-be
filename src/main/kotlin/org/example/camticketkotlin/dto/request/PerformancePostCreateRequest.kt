package org.example.camticketkotlin.dto.request

import org.example.camticketkotlin.domain.enums.PerformanceCategory
import org.example.camticketkotlin.domain.enums.PerformanceLocation
import org.example.camticketkotlin.domain.enums.TicketType
import java.time.LocalDateTime

data class PerformancePostCreateRequest(
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

    val schedules: List<ScheduleRequest>,         // 공연 회차들
    val seatUnavailableCodesPerSchedule: List<SeatUnavailableScheduleRequest>,  // 회차별 예매 불가 좌석
    val ticketOptions: List<TicketOptionRequest>, // 티켓 등급 및 가격
)



