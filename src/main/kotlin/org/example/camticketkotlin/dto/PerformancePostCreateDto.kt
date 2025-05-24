package org.example.camticketkotlin.dto

import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.dto.request.PerformanceImageRequest
import org.example.camticketkotlin.dto.request.PerformancePostCreateRequest
import org.example.camticketkotlin.dto.request.ScheduleRequest
import org.example.camticketkotlin.dto.request.TicketOptionRequest
import java.time.LocalDateTime
import org.example.camticketkotlin.dto.request.*

data class PerformancePostCreateDto(
    val title: String,
    val category: String,
    val location: String,
    val ticketType: String,
    val maxTicketsPerUser: Int,
    val backAccount: String,
    val reservationStartAt: LocalDateTime,
    val reservationEndAt: LocalDateTime,
    val timeNotice: String,
    val priceNotice: String,
    val reservationNotice: String,


    val schedules: List<ScheduleRequest>,
    val seatUnavailableCodesPerSchedule: List<SeatUnavailableScheduleRequest>, // ✅ 회차별 좌석정보 추가

    val ticketOptions: List<TicketOptionRequest>,

    val detailImages: List<PerformanceImageRequest>,
    val profileImageUrl: String,

    val user: User
) {
    companion object {
        fun toDto(
            request: PerformancePostCreateRequest,
            user: User,
            profileImageUrl: String, // 프로필 이미지 추가
            detailImageUrls: List<String> // 상세 이미지 S3 업로드 결과 리스트
        ): PerformancePostCreateDto {
            return PerformancePostCreateDto(
                title = request.title,
                category = request.category.name,
                location = request.location.name,
                ticketType = request.ticketType.name,
                maxTicketsPerUser = request.maxTicketsPerUser,
                backAccount = request.backAccount,
                reservationStartAt = request.reservationStartAt,
                reservationEndAt = request.reservationEndAt,
                timeNotice = request.timeNotice,
                priceNotice = request.priceNotice,
                reservationNotice = request.reservationNotice,

                schedules = request.schedules,
                seatUnavailableCodesPerSchedule = request.seatUnavailableCodesPerSchedule,
                ticketOptions = request.ticketOptions,

                profileImageUrl = profileImageUrl,
                detailImages = detailImageUrls.map { PerformanceImageRequest(it) }, // 변환
                user = user
            )
        }
    }
}



