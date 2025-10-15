package org.example.camticketkotlin.domain

import jakarta.persistence.*

@Entity
class ReservationSeat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_request_id", nullable = false)
    var reservationRequest: ReservationRequest,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_seat_id", nullable = false)
    var scheduleSeat: ScheduleSeat
) : BaseEntity() {

    // ========== DDD: 도메인 로직 ========== //

    /**
     * 좌석 코드 조회
     */
    fun getSeatCode(): String {
        return scheduleSeat.seatCode
    }

    /**
     * 좌석 상태 조회
     */
    fun getSeatStatus(): SeatStatus {
        return scheduleSeat.status
    }

    /**
     * 특정 예매에 속하는지 확인
     */
    fun belongsTo(reservationRequestId: Long): Boolean {
        return this.reservationRequest.id == reservationRequestId
    }

    /**
     * 특정 사용자의 예매인지 확인
     */
    fun isOwnedBy(userId: Long): Boolean {
        return this.reservationRequest.isOwnedBy(userId)
    }

    /**
     * 예약 상태 확인
     */
    fun isReserved(): Boolean {
        return scheduleSeat.isReserved()
    }

    /**
     * 대기 상태 확인
     */
    fun isPending(): Boolean {
        return scheduleSeat.isPending()
    }

    companion object {
        /**
         * 팩토리 메서드: 예매-좌석 연결 생성
         */
        fun create(
            reservationRequest: ReservationRequest,
            scheduleSeat: ScheduleSeat
        ): ReservationSeat {
            // 좌석이 이미 예약되었거나 선택 불가 상태면 예외
            require(scheduleSeat.isPending() || scheduleSeat.isReserved()) {
                "예매할 수 없는 좌석입니다. 좌석 코드: ${scheduleSeat.seatCode}, 상태: ${scheduleSeat.status}"
            }

            return ReservationSeat(
                reservationRequest = reservationRequest,
                scheduleSeat = scheduleSeat
            )
        }
    }
}
