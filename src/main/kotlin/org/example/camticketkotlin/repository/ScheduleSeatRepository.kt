package org.example.camticketkotlin.repository

import jakarta.persistence.LockModeType
import org.example.camticketkotlin.domain.PerformancePost
import org.example.camticketkotlin.domain.PerformanceSchedule
import org.example.camticketkotlin.domain.ScheduleSeat
import org.example.camticketkotlin.domain.SeatStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface ScheduleSeatRepository : JpaRepository<ScheduleSeat, Long> {
    fun findByPerformanceScheduleIn(schedules: List<PerformanceSchedule>): List<ScheduleSeat>
    fun deleteAllByPerformanceScheduleIn(schedules: List<PerformanceSchedule>)

    // 일반 조회 (락 없음)
    fun findByPerformanceScheduleAndSeatCode(schedule: PerformanceSchedule, seatCode: String): ScheduleSeat?

    // 비관적 락을 사용한 좌석 조회 (동시성 제어)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ScheduleSeat s WHERE s.performanceSchedule = :schedule AND s.seatCode = :seatCode")
    fun findByScheduleAndSeatCodeWithLock(schedule: PerformanceSchedule, seatCode: String): ScheduleSeat?

    // 비관적 락을 사용한 스케줄의 모든 좌석 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ScheduleSeat s WHERE s.performanceSchedule = :schedule")
    fun findByScheduleWithLock(schedule: PerformanceSchedule): List<ScheduleSeat>

    // 고아 좌석 조회: 예매와 연결되지 않고 오래된 PENDING 좌석
    // (예매 신청 전 이탈한 경우)
    @Query("""
        SELECT s FROM ScheduleSeat s
        WHERE s.status = :status
        AND s.regDate < :cutoffTime
        AND s.id NOT IN (
            SELECT rs.scheduleSeat.id
            FROM ReservationSeat rs
        )
    """)
    fun findOrphanedSeats(
        status: SeatStatus,
        cutoffTime: LocalDateTime
    ): List<ScheduleSeat>
}


