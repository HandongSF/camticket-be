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
    
    // 공연 정보
    val performanceInfo: PerformanceInfo,
    
    // 예매자 정보
    val reservationInfo: ReservationInfo,
    
    // 좌석 정보
    val seatInfo: SeatInfo,
    
    // 결제 정보
    val paymentInfo: List<PaymentInfoItem>,  // 배열로 변경
    
    // 예매 날짜
    val reservationDate: LocalDateTime
) {
    data class PerformanceInfo(
        val title: String,
        val category: PerformanceCategory,
        val location: PerformanceLocation,
        val performanceDate: LocalDateTime,
        val scheduleId: Long,  // ← 추가
        val scheduleIndex: Int?, // ← 추가 (몇 회차인지)
        val profileImageUrl: String
    )

    data class ReservationInfo(
        val userNickName: String,
        val userBankAccount: String,  // ← 추가
        val ticketCount: Int,
        val totalPrice: Int,  // ← 전체 총액 추가
        val isPaymentCompleted: Boolean  // ← 여기로 이동
    )
    
    data class SeatInfo(
        val ticketType: TicketType,
        val selectedSeats: List<String>
    )
    data class PaymentInfoItem(
        val ticketOptionName: String,
        val unitPrice: Int,
        val quantity: Int,
        val subtotal: Int
    )
}