package org.example.camticketkotlin.dto.response

import org.example.camticketkotlin.domain.ReservationStatus
import org.example.camticketkotlin.domain.enums.PerformanceCategory
import org.example.camticketkotlin.domain.enums.PerformanceLocation
import java.time.LocalDateTime

data class AdminReservationDetailResponse(
    // 예매 기본 정보
    val reservationId: Long,
    val status: ReservationStatus,
    val statusDescription: String,

    // 공연 정보 (관리자는 더 상세한 정보 필요)
    val performanceInfo: AdminPerformanceInfo,

    // 예매자 상세 정보
    val customerInfo: CustomerInfo,

    // 좌석 정보
    val seatInfo: ReservationDetailResponse.SeatInfo,

    // 결제 정보
    val paymentInfo: ReservationDetailResponse.PaymentInfo,

    // 관리 정보
    val managementInfo: ManagementInfo
) {
    data class AdminPerformanceInfo(
        val performanceId: Long,
        val title: String,
        val category: PerformanceCategory,
        val location: PerformanceLocation,
        val performanceDate: LocalDateTime,
        val scheduleId: Long,
        val profileImageUrl: String
    )

    data class CustomerInfo(
        val userId: Long,
        val userName: String,
        val userEmail: String,
        val userNickname: String?
    )

    data class ManagementInfo(
        val reservationDate: LocalDateTime,
        val lastModifiedDate: LocalDateTime,
        val canApprove: Boolean,      // 승인 가능 여부
        val canReject: Boolean,       // 거절 가능 여부
        val canRefund: Boolean,       // 환불 가능 여부
        val notes: String             // 관리자 참고사항
    )
}