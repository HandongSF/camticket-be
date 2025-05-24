package org.example.camticketkotlin.domain

import jakarta.persistence.*

@Entity
class PerformanceImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var imageUrl: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_post_id", nullable = false)
    var performancePost: PerformancePost
)
