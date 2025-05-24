package org.example.camticketkotlin.repository

import org.example.camticketkotlin.domain.PerformancePost
import org.example.camticketkotlin.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface PerformancePostRepository : JpaRepository<PerformancePost, Long>{
    fun findAllByUserId(userId: Long): List<PerformancePost>

}

