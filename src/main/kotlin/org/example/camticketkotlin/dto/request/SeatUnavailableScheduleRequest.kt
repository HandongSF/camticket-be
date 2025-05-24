package org.example.camticketkotlin.dto.request


data class SeatUnavailableScheduleRequest(
    val scheduleIndex: Int,
    val codes: List<String>
)
