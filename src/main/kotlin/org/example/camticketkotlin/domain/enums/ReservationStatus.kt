package org.example.camticketkotlin.domain

enum class ReservationStatus {
    PENDING,           // 예매 대기 (입금 확인 중)
    APPROVED,          // 예매 승인 (입금 완료)
    REJECTED,          // 예매 거절
    REFUND_REQUESTED,  // 환불 신청 (관리자 승인 대기)
    REFUNDED           // 환불 완료
}