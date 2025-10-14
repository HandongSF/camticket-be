package org.example.camticketkotlin.scheduler

import org.example.camticketkotlin.domain.SeatStatus
import org.example.camticketkotlin.repository.ScheduleSeatRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 고아 좌석 자동 해제 스케줄러
 *
 * 시나리오:
 * 1. 사용자가 좌석 선택 (PENDING 상태로 즉시 커밋)
 * 2. 예매 신청 전에 이탈 (브라우저 닫기, 뒤로가기, 네트워크 끊김 등)
 * 3. 좌석만 PENDING 상태로 남음 (고아 좌석)
 * 4. 이 스케줄러가 일정 시간 후 자동 해제
 *
 * 주의:
 * - 예매가 완료된 PENDING 좌석은 해제하지 않음
 * - 오직 예매와 연결되지 않은(orphaned) 좌석만 해제
 */
@Component
class SeatCleanupScheduler(
    private val scheduleSeatRepository: ScheduleSeatRepository
) {
    private val logger = LoggerFactory.getLogger(SeatCleanupScheduler::class.java)

    companion object {
        // 고아 좌석 타임아웃: 10분
        const val ORPHAN_SEAT_TIMEOUT_MINUTES = 10L
    }

    /**
     * 고아 좌석 자동 해제
     * - 1분마다 실행
     * - 10분 이상 예매와 연결되지 않은 PENDING 좌석을 해제
     */
    @Scheduled(fixedDelay = 60000) // 1분 = 60,000ms
    @Transactional
    fun releaseOrphanedSeats() {
        try {
            val cutoffTime = LocalDateTime.now().minusMinutes(ORPHAN_SEAT_TIMEOUT_MINUTES)

            // 예매와 연결되지 않은 오래된 PENDING 좌석 조회
            val orphanedSeats = scheduleSeatRepository.findOrphanedSeats(
                status = SeatStatus.PENDING,
                cutoffTime = cutoffTime
            )

            if (orphanedSeats.isNotEmpty()) {
                logger.info("고아 좌석 발견: ${orphanedSeats.size}개")

                orphanedSeats.forEach { seat ->
                    logger.info(
                        "고아 좌석 해제: [스케줄=${seat.performanceSchedule.id}, " +
                        "좌석=${seat.seatCode}, 생성시간=${seat.regDate}]"
                    )
                    scheduleSeatRepository.delete(seat)
                }

                logger.info("고아 좌석 ${orphanedSeats.size}개 해제 완료")
            }
        } catch (e: Exception) {
            logger.error("고아 좌석 해제 중 오류 발생", e)
            // 스케줄러 에러는 로그만 남기고 계속 진행
        }
    }
}
