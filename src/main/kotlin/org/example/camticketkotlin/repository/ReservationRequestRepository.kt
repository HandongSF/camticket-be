// 1. ReservationRequestRepository.kt
package org.example.camticketkotlin.repository

import org.example.camticketkotlin.domain.ReservationRequest
import org.example.camticketkotlin.domain.ReservationStatus
import org.example.camticketkotlin.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ReservationRequestRepository : JpaRepository<ReservationRequest, Long> {

    // 사용자별 예매 신청 목록 조회
    fun findByUserOrderByRegDateDesc(user: User): List<ReservationRequest>

    // 특정 상태의 예매 신청 조회
    fun findByStatus(status: ReservationStatus): List<ReservationRequest>

    // 특정 공연 회차의 예매 신청 조회 (최신순)
    fun findByPerformanceScheduleIdOrderByRegDateDesc(scheduleId: Long): List<ReservationRequest>

    // 사용자가 특정 공연에 예매한 수량 합계 조회
    @Query("""
        SELECT COALESCE(SUM(r.count), 0) 
        FROM ReservationRequest r 
        WHERE r.user = :user 
        AND r.performanceSchedule.performancePost.id = :postId
        AND r.status IN ('PENDING', 'APPROVED')
    """)
    fun getTotalReservationCountByUserAndPost(user: User, postId: Long): Int

    // 특정 회차의 예매된 좌석 수 조회
    @Query("""
        SELECT COALESCE(SUM(r.count), 0) 
        FROM ReservationRequest r 
        WHERE r.performanceSchedule.id = :scheduleId 
        AND r.status IN ('PENDING', 'APPROVED')
    """)
    fun getReservedSeatCount(scheduleId: Long): Int
}