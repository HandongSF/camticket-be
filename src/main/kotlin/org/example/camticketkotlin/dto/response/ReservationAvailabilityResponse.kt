package org.example.camticketkotlin.dto.response

data class ReservationAvailabilityResponse(
    val isAvailable: Boolean,
    val maxReservableCount: Int,      // 최대 예매 가능 수량
    val currentUserReservationCount: Int,  // 현재 유저가 이미 예매한 수량
    val remainingUserQuota: Int,      // 유저에게 남은 예매 가능 수량
    val message: String               // 안내 메시지
)