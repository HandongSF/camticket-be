package org.example.camticketkotlin.repository

import org.example.camticketkotlin.domain.PerformanceImage
import org.springframework.data.jpa.repository.JpaRepository

interface PerformanceImageRepository : JpaRepository<PerformanceImage, Long>
