/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.camticketkotlin.domain.service

import org.example.camticketkotlin.domain.PerformanceSchedule
import org.example.camticketkotlin.domain.ScheduleSeat
import org.example.camticketkotlin.repository.ScheduleSeatRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.example.camticketkotlin.domain.SeatStatus
import java.time.LocalDateTime

/**
 * 좌석 잠금 도메인 서비스
 *
 * DDD 패턴:
 * - Domain Service: 여러 Aggregate를 조율하거나 복잡한 비즈니스 로직을 처리
 * - 동시성 제어 로직은 도메인의 핵심 관심사이므로 Domain Service로 분리
 *
 * 기술적 전략:
 * - INSERT-first 전략: Gap Lock 데드락 회피
 * - Pessimistic Lock: 동시성 제어
 * - REQUIRES_NEW: 독립 트랜잭션으로 즉시 커밋
 */
@Service
class SeatLockingDomainService(
    private val scheduleSeatRepository: ScheduleSeatRepository
) {

    /**
     * 좌석 잠금 (독립 트랜잭션)
     *
     * 전략:
     * 1. INSERT-first: 먼저 PENDING 상태로 INSERT 시도 (Record Lock, Gap Lock 없음)
     * 2. Unique constraint 위반 시: SELECT FOR UPDATE로 기존 좌석 락 획득
     * 3. 좌석 상태 검증 후 PENDING으로 변경
     *
     * 트랜잭션:
     * - REQUIRES_NEW: 즉시 커밋하여 다른 사용자에게 실시간으로 PENDING 상태 반영
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun lockSeats(
        seatCodes: List<String>,
        schedule: PerformanceSchedule
    ): List<ScheduleSeat> {
        val lockedSeats = mutableListOf<ScheduleSeat>()

        seatCodes.forEach { seatCode ->
            val lockedSeat = tryLockSeat(seatCode, schedule)
            lockedSeats.add(lockedSeat)
        }

        return lockedSeats
    }

    /**
     * 개별 좌석 잠금 시도
     */
    private fun tryLockSeat(
        seatCode: String,
        schedule: PerformanceSchedule
    ): ScheduleSeat {
        return try {
            // ① INSERT-first 전략: 먼저 PENDING으로 INSERT 시도
            // createPending() 팩토리 메서드가 이미 PENDING 상태로 생성함
            val newSeat = ScheduleSeat.createPending(seatCode, schedule)
            scheduleSeatRepository.save(newSeat)

        } catch (e: DataIntegrityViolationException) {
            // ② Unique constraint 위반 = 좌석이 이미 존재
            // 이 경우에만 SELECT FOR UPDATE 사용
            val existingSeat = scheduleSeatRepository
                .findByScheduleAndSeatCodeWithLock(schedule, seatCode)
                ?: throw IllegalArgumentException("좌석 조회 실패: $seatCode")

            // 도메인 로직 실행: 좌석 상태 검증 및 락 획득
            // lockForReservation()은 AVAILABLE → PENDING으로 변경
            existingSeat.lockForReservation()

            scheduleSeatRepository.save(existingSeat)
        }
    }

    /**
     * 좌석 해제 (예약 취소/거절 시)
     */
    @Transactional
    fun releaseSeats(seats: List<ScheduleSeat>) {
        seats.forEach { seat ->
            if (seat.isPending()) {
                // PENDING 상태 좌석은 DB에서 삭제 (AVAILABLE로 복원)
                scheduleSeatRepository.delete(seat)
            } else {
                // 다른 상태는 도메인 로직으로 해제
                seat.release()
                scheduleSeatRepository.save(seat)
            }
        }
    }

    /**
     * 예약 확정 (PENDING → RESERVED)
     */
    @Transactional
    fun confirmReservation(seats: List<ScheduleSeat>) {
        seats.forEach { seat ->
            seat.confirmReservation()
            scheduleSeatRepository.save(seat)
        }
    }

    /**
     * 고아 좌석 찾기 (예매와 연결되지 않은 오래된 PENDING 좌석)
     */
    @Transactional(readOnly = true)
    fun findOrphanedSeats(timeoutMinutes: Long): List<ScheduleSeat> {
        val cutoffTime = LocalDateTime.now().minusMinutes(timeoutMinutes)
        return scheduleSeatRepository.findOrphanedSeats(SeatStatus.PENDING, cutoffTime)
    }

    /**
     * 고아 좌석 자동 해제
     */
    @Transactional
    fun releaseOrphanedSeats(orphanedSeats: List<ScheduleSeat>) {
        orphanedSeats.forEach { seat ->
            scheduleSeatRepository.delete(seat)
        }
    }
}