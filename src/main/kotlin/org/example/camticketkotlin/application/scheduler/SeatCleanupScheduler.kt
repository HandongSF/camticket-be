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

package org.example.camticketkotlin.application.scheduler

import org.example.camticketkotlin.domain.service.SeatLockingDomainService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 고아 좌석 자동 해제 스케줄러
 *
 * DDD 패턴:
 * - Application Layer의 Scheduler
 * - Domain Service를 호출하여 비즈니스 로직 실행
 * - 인프라 관심사(스케줄링)와 도메인 로직 분리
 *
 * 시나리오:
 * 1. 사용자가 좌석 선택 (PENDING 상태로 즉시 커밋)
 * 2. 예매 신청 전에 이탈 (브라우저 닫기, 뒤로가기, 네트워크 끊김 등)
 * 3. 좌석만 PENDING 상태로 남음 (고아 좌석)
 * 4. 이 스케줄러가 일정 시간 후 자동 해제
 */
@Component
class SeatCleanupScheduler(
    private val seatLockingDomainService: SeatLockingDomainService
) {
    private val logger = LoggerFactory.getLogger(SeatCleanupScheduler::class.java)

    companion object {
        const val ORPHAN_SEAT_TIMEOUT_MINUTES = 10L
    }

    /**
     * 고아 좌석 자동 해제
     * - 1분마다 실행
     * - 10분 이상 예매와 연결되지 않은 PENDING 좌석을 해제
     */
    @Scheduled(fixedDelay = 60000) // 1분
    fun releaseOrphanedSeats() {
        try {
            // Domain Service 호출
            val orphanedSeats = seatLockingDomainService
                .findOrphanedSeats(ORPHAN_SEAT_TIMEOUT_MINUTES)

            if (orphanedSeats.isNotEmpty()) {
                logger.info("고아 좌석 발견: ${orphanedSeats.size}개")

                orphanedSeats.forEach { seat ->
                    logger.info(
                        "고아 좌석 해제: [스케줄=${seat.performanceSchedule.id}, " +
                        "좌석=${seat.seatCode}, 생성시간=${seat.regDate}]"
                    )
                }

                // Domain Service 호출
                seatLockingDomainService.releaseOrphanedSeats(orphanedSeats)

                logger.info("고아 좌석 ${orphanedSeats.size}개 해제 완료")
            }
        } catch (e: Exception) {
            logger.error("고아 좌석 해제 중 오류 발생", e)
        }
    }
}