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
) : BaseEntity()
