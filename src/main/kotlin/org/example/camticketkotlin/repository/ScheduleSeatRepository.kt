package org.example.camticketkotlin.repository

import org.example.camticketkotlin.domain.ScheduleSeat
import org.springframework.data.jpa.repository.JpaRepository

interface ScheduleSeatRepository : JpaRepository<ScheduleSeat, Long>
