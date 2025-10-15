package org.example.camticketkotlin.domain

import jakarta.persistence.*

@Entity
class ReservationRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_schedule_id", nullable = false)
    var performanceSchedule: PerformanceSchedule,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_option_id", nullable = false)
    var ticketOption: TicketOption,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false)
    var count: Int,  // 예매 수량

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReservationStatus,  // PENDING / APPROVED / REJECTED

    @Column(nullable = false)
    var isPaymentCompleted: Boolean = false  // ← 추가: 사용자가 입금했다고 체크했는지

) : BaseEntity() {

    // ========== DDD: 도메인 로직 ========== //

    /**
     * 예매 승인
     */
    fun approve() {
        require(status == ReservationStatus.PENDING) { "대기 중인 예매만 승인할 수 있습니다. 현재 상태: $status" }
        require(isPaymentCompleted) { "입금 완료된 예매만 승인할 수 있습니다." }
        this.status = ReservationStatus.APPROVED
    }

    /**
     * 예매 거절
     */
    fun reject() {
        require(status == ReservationStatus.PENDING) { "대기 중인 예매만 거절할 수 있습니다. 현재 상태: $status" }
        this.status = ReservationStatus.REJECTED
    }

    /**
     * 환불 신청
     */
    fun requestRefund() {
        require(status == ReservationStatus.APPROVED) { "승인된 예매만 환불 신청할 수 있습니다. 현재 상태: $status" }
        this.status = ReservationStatus.REFUND_REQUESTED
    }

    /**
     * 환불 완료 처리
     */
    fun completeRefund() {
        require(status == ReservationStatus.REFUND_REQUESTED) { "환불 신청된 예매만 환불 완료 처리할 수 있습니다. 현재 상태: $status" }
        this.status = ReservationStatus.REFUNDED
    }

    /**
     * 입금 완료 체크
     */
    fun markPaymentCompleted() {
        require(!isPaymentCompleted) { "이미 입금 완료 상태입니다." }
        this.isPaymentCompleted = true
    }

    /**
     * 상태 검증 메서드
     */
    fun isPending(): Boolean = status == ReservationStatus.PENDING
    fun isApproved(): Boolean = status == ReservationStatus.APPROVED
    fun isRejected(): Boolean = status == ReservationStatus.REJECTED
    fun isRefundRequested(): Boolean = status == ReservationStatus.REFUND_REQUESTED
    fun isRefunded(): Boolean = status == ReservationStatus.REFUNDED

    /**
     * 수정 가능한 상태인지 확인
     */
    fun canBeModified(): Boolean {
        return status == ReservationStatus.PENDING
    }

    /**
     * 취소 가능한 상태인지 확인
     */
    fun canBeCancelled(): Boolean {
        return status in listOf(ReservationStatus.PENDING, ReservationStatus.APPROVED)
    }

    /**
     * 예매자 본인 확인
     */
    fun isOwnedBy(userId: Long): Boolean {
        return this.user.id == userId
    }

    /**
     * 총 금액 계산
     */
    fun calculateTotalAmount(): Int {
        return ticketOption.price * count
    }

    companion object {
        /**
         * 팩토리 메서드: 새 예매 생성
         */
        fun create(
            user: User,
            performanceSchedule: PerformanceSchedule,
            ticketOption: TicketOption,
            count: Int,
            isPaymentCompleted: Boolean
        ): ReservationRequest {
            require(count > 0) { "예매 수량은 1 이상이어야 합니다." }

            return ReservationRequest(
                user = user,
                performanceSchedule = performanceSchedule,
                ticketOption = ticketOption,
                count = count,
                status = ReservationStatus.PENDING,
                isPaymentCompleted = isPaymentCompleted
            )
        }
    }
}
