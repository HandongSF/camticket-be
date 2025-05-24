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
    var status: ReservationStatus  // PENDING / APPROVED / REJECTED
) : BaseEntity()
