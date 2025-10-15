package org.example.camticketkotlin.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 좌석 Aggregate Root (DDD Rich Domain Model)
 *
 * 비즈니스 로직을 도메인 모델에 캡슐화
 * - 상태 변경은 도메인 메서드를 통해서만 가능
 * - 불변 규칙을 도메인 로직으로 강제
 */
@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_schedule_seat_code",
            columnNames = ["performance_schedule_id", "seat_code"]
        )
    ]
)
class ScheduleSeat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var seatCode: String,  // e.g., "A1", "B3"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SeatStatus,  // AVAILABLE / UNAVAILABLE / RESERVED / PENDING

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_schedule_id", nullable = false)
    var performanceSchedule: PerformanceSchedule
) : BaseEntity() {

    // ========== DDD: 도메인 로직 ========== //

    /**
     * 좌석 잠금 (AVAILABLE → PENDING)
     *
     * 비즈니스 규칙:
     * - AVAILABLE 상태에서만 잠금 가능
     * - 이미 선택/예약/사용불가 상태면 예외 발생
     */
    fun lockForReservation() {
        when (status) {
            SeatStatus.AVAILABLE -> status = SeatStatus.PENDING
            SeatStatus.PENDING -> throw IllegalStateException("이미 선택 중인 좌석입니다: $seatCode")
            SeatStatus.RESERVED -> throw IllegalStateException("이미 예약된 좌석입니다: $seatCode")
            SeatStatus.UNAVAILABLE -> throw IllegalStateException("선택할 수 없는 좌석입니다: $seatCode")
        }
    }

    /**
     * 예약 확정 (PENDING → RESERVED)
     */
    fun confirmReservation() {
        require(status == SeatStatus.PENDING) {
            "PENDING 상태의 좌석만 예약 확정할 수 있습니다. 현재 상태: $status"
        }
        status = SeatStatus.RESERVED
    }

    /**
     * 좌석 해제 (→ AVAILABLE)
     */
    fun release() {
        when (status) {
            SeatStatus.PENDING, SeatStatus.RESERVED -> status = SeatStatus.AVAILABLE
            SeatStatus.AVAILABLE -> { /* 이미 해제됨 */ }
            SeatStatus.UNAVAILABLE -> throw IllegalStateException("사용 불가 좌석은 해제할 수 없습니다: $seatCode")
        }
    }

    /**
     * 사용 불가 처리
     */
    fun markAsUnavailable() {
        status = SeatStatus.UNAVAILABLE
    }

    /**
     * 선택 가능한 상태인지 확인
     */
    fun isAvailable(): Boolean = status == SeatStatus.AVAILABLE

    /**
     * PENDING 상태인지 확인
     */
    fun isPending(): Boolean = status == SeatStatus.PENDING

    /**
     * 예약 완료 상태인지 확인
     */
    fun isReserved(): Boolean = status == SeatStatus.RESERVED

    /**
     * 고아 좌석인지 확인 (생성 후 일정 시간 경과)
     */
    fun isOrphaned(timeoutMinutes: Long): Boolean {
        if (status != SeatStatus.PENDING) return false
        val createdAt = regDate ?: return false
        val cutoffTime = LocalDateTime.now().minusMinutes(timeoutMinutes)
        return createdAt.isBefore(cutoffTime)
    }

    companion object {
        /**
         * 팩토리 메서드: PENDING 상태 좌석 생성
         */
        fun createPending(
            seatCode: String,
            schedule: PerformanceSchedule
        ): ScheduleSeat {
            return ScheduleSeat(
                seatCode = seatCode,
                status = SeatStatus.PENDING,
                performanceSchedule = schedule
            )
        }

        /**
         * 팩토리 메서드: UNAVAILABLE 상태 좌석 생성
         */
        fun createUnavailable(
            seatCode: String,
            schedule: PerformanceSchedule
        ): ScheduleSeat {
            return ScheduleSeat(
                seatCode = seatCode,
                status = SeatStatus.UNAVAILABLE,
                performanceSchedule = schedule
            )
        }
    }
}
