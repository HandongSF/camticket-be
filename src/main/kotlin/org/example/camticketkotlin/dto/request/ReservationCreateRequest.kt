package org.example.camticketkotlin.dto.request

data class ReservationCreateRequest(
    val performancePostId: Long,
    val performanceScheduleId: Long,
    val selectedSeatCodes: List<String> = emptyList(),
    val isPaymentCompleted: Boolean,
    val userBankAccount: String,
    val ticketOrders: List<TicketOrderItem>  // ← 여러 티켓 옵션 지원
)

data class TicketOrderItem(
    val ticketOptionId: Long,
    val count: Int,
    val unitPrice: Int  // 검증용
)