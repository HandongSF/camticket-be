package org.example.camticketkotlin.domain

import jakarta.persistence.*

@Entity
class TicketOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50)
    var name: String,  // e.g., 일반 or 새내기

    @Column(nullable = false)
    var price: Int,  // 단위: 원

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_post_id", nullable = false)
    var performancePost: PerformancePost
)
