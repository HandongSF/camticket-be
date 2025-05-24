package org.example.camticketkotlin.repository

import org.example.camticketkotlin.domain.PerformanceImage
import org.example.camticketkotlin.domain.PerformancePost
import org.springframework.data.jpa.repository.JpaRepository

interface PerformanceImageRepository : JpaRepository<PerformanceImage, Long> {
    fun findByPerformancePost(performancePost: PerformancePost): List<PerformanceImage>
    fun deleteAllByPerformancePost(performancePost: PerformancePost)
}

