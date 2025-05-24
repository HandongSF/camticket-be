package org.example.camticketkotlin.repository

import org.example.camticketkotlin.domain.PerformanceSchedule
import org.springframework.data.jpa.repository.JpaRepository

interface PerformanceScheduleRepository : JpaRepository<PerformanceSchedule, Long>
