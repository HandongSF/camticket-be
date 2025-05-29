package org.example.camticketkotlin.dto.response

import org.example.camticketkotlin.domain.ReservationStatus
import org.example.camticketkotlin.domain.enums.PerformanceCategory
import org.example.camticketkotlin.domain.enums.PerformanceLocation
import org.example.camticketkotlin.domain.enums.TicketType
import java.time.LocalDateTime

data class ReservationDetailResponse(
    // 예매 기본 정보
    val reservationId: Long,
    val status: ReservationStatus,
    val statusDescription: String,  // 상태 설명 (예: "예매 완료", "환불 신청 중", "예매 취소 가능")
    val actionType: String,         // "CANCEL" 또는 "REFUND" 또는 "NONE"
    val actionDescription: String,  // "예매 취소", "환불 신청", "처리 완료"
    
    // 공연 정보
    val performanceInfo: PerformanceInfo,
    
    // 예매자 정보
    val reservationInfo: ReservationInfo,
    
    // 좌석 정보
    val seatInfo: SeatInfo,
    
    // 결제 정보
    val paymentInfo: PaymentInfo,
    
    // 예매 날짜
    val reservationDate: LocalDateTime
) {
    data class PerformanceInfo(
        val title: String,
        val category: PerformanceCategory,
        val location: PerformanceLocation,
        val performanceDate: LocalDateTime,
        val profileImageUrl: String
    )
    
    data class ReservationInfo(
        val userNickName: String,
        val userEmail: String,
        val ticketCount: Int
    )
    
    data class SeatInfo(
        val ticketType: TicketType,
        val selectedSeats: List<String>,
        val seatDescription: String  // "지정석: A1, A2" 또는 "자유석 2매"
    )
    
    data class PaymentInfo(
        val ticketOptionName: String,
        val unitPrice: Int,
        val quantity: Int,
        val totalPrice: Int,
        val priceDescription: String  // "일반 4,000원 × 2매 = 8,000원"
    )
}