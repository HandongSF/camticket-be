package org.example.camticketkotlin.dto.response

data class PerformancePostDetailResponse(
    val id: Long,
    val title: String,
    val category: String,
    val location: String,
    val ticketType: String,
    val maxTicketsPerUser: Int,
    val backAccount: String,
    val reservationStartAt: String,
    val reservationEndAt: String,
    val timeNotice: String,
    val priceNotice: String,
    val reservationNotice: String,
    val profileImageUrl: String,
    val detailImageUrls: List<String>,
    val schedules: List<ScheduleDto>,
    val seatUnavailableCodesPerSchedule: List<SeatUnavailableDto>,
    val ticketOptions: List<TicketOptionDto>
) {
    data class ScheduleDto(val scheduleIndex: Int, val startTime: String)
    data class SeatUnavailableDto(val scheduleIndex: Int, val codes: List<String>)
    data class TicketOptionDto(val name: String, val price: Int)
}
