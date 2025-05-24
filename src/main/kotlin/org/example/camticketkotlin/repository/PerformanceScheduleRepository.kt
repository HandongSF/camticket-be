package org.example.camticketkotlin.repository

import org.example.camticketkotlin.domain.PerformancePost
import org.example.camticketkotlin.domain.PerformanceSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PerformanceScheduleRepository : JpaRepository<PerformanceSchedule, Long>{
    fun findByPerformancePost(performancePost: PerformancePost): List<PerformanceSchedule>

    @Query("""
    SELECT ps.performancePost.id, MAX(ps.startTime)
    FROM PerformanceSchedule ps
    WHERE ps.performancePost.id IN :postIds
    GROUP BY ps.performancePost.id
""")
    fun findLatestScheduleTimes(postIds: List<Long>): List<Array<Any>>
    fun deleteAllByPerformancePost(performancePost: PerformancePost)
}

