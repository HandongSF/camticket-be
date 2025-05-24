package org.example.camticketkotlin.repository

import org.example.camticketkotlin.domain.PerformanceSchedule
import org.example.camticketkotlin.domain.ScheduleSeat
import org.springframework.data.jpa.repository.JpaRepository

interface ScheduleSeatRepository : JpaRepository<ScheduleSeat, Long> {
    fun findByPerformanceScheduleIn(schedules: List<PerformanceSchedule>): List<ScheduleSeat>
}

