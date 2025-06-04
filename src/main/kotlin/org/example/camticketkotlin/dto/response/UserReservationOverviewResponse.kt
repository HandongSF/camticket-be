package org.example.camticketkotlin.dto.response

import org.example.camticketkotlin.domain.ReservationStatus
import org.example.camticketkotlin.domain.enums.PerformanceLocation
import java.time.LocalDateTime

data class UserReservationOverviewResponse(
    val reservationId: Long,
    val performanceTitle: String,
    val performanceDate: LocalDateTime,
    val ticketOptionName: String,
    val ticketPrice: Int,
    val count: Int,
    val totalPrice: Int,
    val status: ReservationStatus,
    val selectedSeats: List<String>,  // 선택한 좌석 코드들
    val regDate: LocalDateTime,
    
    // ✅ UI에 필요한 추가 정보들
    val reservationStartAt: LocalDateTime,    // 예매 시작일
    val reservationEndAt: LocalDateTime,      // 예매 마감일
    val location: PerformanceLocation,        // 공연 장소
    val locationDisplayName: String,          // 공연 장소 한글명
    val performanceProfileImageUrl: String,   // 공연 게시물 프로필 사진
    val artistId: Long,                       // 공연 게시물 올린 아티스트 ID
    val artistName: String?,                  // 아티스트 이름
    val artistProfileImageUrl: String?        // 아티스트 프로필 사진
)