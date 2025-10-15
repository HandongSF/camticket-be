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

    // ========== DDD: 도메인 로직 ========== //

    /**
     * 공연 시작 전인지 확인
     */
    fun isBeforeStart(): Boolean {
        return LocalDateTime.now().isBefore(startTime)
    }

    /**
     * 공연 시작 후인지 확인
     */
    fun isAfterStart(): Boolean {
        return LocalDateTime.now().isAfter(startTime)
    }

    /**
     * 공연이 시작되었는지 확인 (예외 발생)
     */
    fun validateNotStarted() {
        require(isBeforeStart()) { "이미 시작된 공연은 수정할 수 없습니다. 시작 시간: $startTime" }
    }

    /**
     * 공연 시작 시간 변경
     */
    fun updateStartTime(newStartTime: LocalDateTime) {
        validateNotStarted()
        require(newStartTime.isAfter(LocalDateTime.now())) { "공연 시작 시간은 현재 시간 이후여야 합니다." }
        this.startTime = newStartTime
    }

    /**
     * 특정 시간까지 몇 시간 남았는지 계산
     */
    fun getHoursUntilStart(): Long {
        val now = LocalDateTime.now()
        return if (now.isBefore(startTime)) {
            java.time.Duration.between(now, startTime).toHours()
        } else {
            0L
        }
    }

    @Deprecated("Use constructor or factory method instead")
    fun toEntity(post: PerformancePost): PerformanceSchedule {
        return PerformanceSchedule(
            startTime = this.startTime,
            performancePost = post
        )
    }

    companion object {
        /**
         * 팩토리 메서드: 새 공연 회차 생성
         */
        fun create(performancePost: PerformancePost, startTime: LocalDateTime): PerformanceSchedule {
            require(startTime.isAfter(LocalDateTime.now())) { "공연 시작 시간은 현재 시간 이후여야 합니다." }
            return PerformanceSchedule(
                startTime = startTime,
                performancePost = performancePost
            )
        }
    }
}