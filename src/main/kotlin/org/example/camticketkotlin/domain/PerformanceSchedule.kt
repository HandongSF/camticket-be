package org.example.camticketkotlin.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class PerformanceSchedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var startTime: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_post_id", nullable = false)
    var performancePost: PerformancePost
)  {
    fun toEntity(post: PerformancePost): PerformanceSchedule {
        return PerformanceSchedule(
            startTime = this.startTime,
            performancePost = post
        )
    }

}