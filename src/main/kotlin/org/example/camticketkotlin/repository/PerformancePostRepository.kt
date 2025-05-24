package org.example.camticketkotlin.repository

import org.example.camticketkotlin.domain.PerformancePost
import org.springframework.data.jpa.repository.JpaRepository

interface PerformancePostRepository : JpaRepository<PerformancePost, Long>
