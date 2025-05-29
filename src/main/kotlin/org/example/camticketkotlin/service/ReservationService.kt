package org.example.camticketkotlin.service

import org.example.camticketkotlin.domain.*
import org.example.camticketkotlin.dto.request.ReservationCreateRequest
import org.example.camticketkotlin.dto.response.*
import org.example.camticketkotlin.exception.NotFoundException
import org.example.camticketkotlin.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ReservationService(
    private val reservationRequestRepository: ReservationRequestRepository,
    private val reservationSeatRepository: ReservationSeatRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val performancePostRepository: PerformancePostRepository,
    private val ticketOptionRepository: TicketOptionRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository
) {

    // 1. 공연 회차 목록 조회
    @Transactional(readOnly = true)
    fun getPerformanceSchedules(postId: Long): List<PerformanceScheduleResponse> {
        val post = performancePostRepository.findById(postId)
            .orElseThrow { NotFoundException("해당 공연이 존재하지 않습니다.") }

        val schedules = performanceScheduleRepository.findByPerformancePost(post)

        return schedules.map { schedule ->
            val reservedCount = reservationRequestRepository.getReservedSeatCount(schedule.id!!)
            val totalSeats = calculateTotalSeats(schedule) // 전체 좌석 수 계산
            val availableSeats = totalSeats - reservedCount

            PerformanceScheduleResponse(
                scheduleId = schedule.id!!,
                startTime = schedule.startTime,
                availableSeats = availableSeats,
                totalSeats = totalSeats,
                isBookingAvailable = availableSeats > 0 &&
                        post.reservationStartAt <= LocalDateTime.now() &&
                        post.reservationEndAt >= LocalDateTime.now()
            )
        }
    }

    // 2. 좌석 정보 조회 (지정석인 경우) - 예외 상태만 반환
    @Transactional(readOnly = true)
    fun getSeatInfo(scheduleId: Long): List<SeatInfoResponse> {
        val schedule = performanceScheduleRepository.findById(scheduleId)
            .orElseThrow { NotFoundException("해당 공연 회차가 존재하지 않습니다.") }

        // 지정석이 아닌 경우 빈 리스트 반환
        if (schedule.performancePost.ticketType.name != "PAID") {
            return emptyList()
        }

        // DB에서 예외 상태 좌석들만 조회하여 반환
        val exceptionSeats = scheduleSeatRepository.findByPerformanceScheduleIn(listOf(schedule))

        return exceptionSeats.map { seat ->
            SeatInfoResponse(
                seatCode = seat.seatCode,
                status = seat.status
            )
        }
    }

    // 3. 티켓 옵션 조회
    @Transactional(readOnly = true)
    fun getTicketOptions(postId: Long): List<TicketOptionResponse> {
        val post = performancePostRepository.findById(postId)
            .orElseThrow { NotFoundException("해당 공연이 존재하지 않습니다.") }

        val options = ticketOptionRepository.findByPerformancePost(post)

        return options.map { option ->
            TicketOptionResponse(
                optionId = option.id!!,
                name = option.name,
                price = option.price,
                availableCount = calculateAvailableCount(option, post)
            )
        }
    }

    // 4. 예매 가능 여부 체크
    @Transactional(readOnly = true)
    fun checkReservationAvailability(user: User, postId: Long): ReservationAvailabilityResponse {
        val post = performancePostRepository.findById(postId)
            .orElseThrow { NotFoundException("해당 공연이 존재하지 않습니다.") }

        val currentTime = LocalDateTime.now()
        val userReservationCount = reservationRequestRepository
            .getTotalReservationCountByUserAndPost(user, postId)

        val remainingQuota = post.maxTicketsPerUser - userReservationCount

        return when {
            currentTime < post.reservationStartAt -> ReservationAvailabilityResponse(
                false, 0, userReservationCount, 0,
                "예매 시작 시간이 아닙니다."
            )
            currentTime > post.reservationEndAt -> ReservationAvailabilityResponse(
                false, 0, userReservationCount, 0,
                "예매 마감되었습니다."
            )
            remainingQuota <= 0 -> ReservationAvailabilityResponse(
                false, 0, userReservationCount, 0,
                "예매 가능 수량을 모두 사용했습니다."
            )
            else -> ReservationAvailabilityResponse(
                true, post.maxTicketsPerUser, userReservationCount, remainingQuota,
                "예매 가능합니다."
            )
        }
    }

    // 5. 예매 신청
    fun createReservation(user: User, request: ReservationCreateRequest): ReservationResponse {
        // 1. 공연 게시글 유효성 검사
        val post = performancePostRepository.findById(request.performancePostId)
            .orElseThrow { NotFoundException("해당 공연이 존재하지 않습니다.") }

        // 2. 회차가 해당 공연에 속하는지 확인
        val schedule = performanceScheduleRepository.findById(request.performanceScheduleId)
            .orElseThrow { NotFoundException("해당 공연 회차가 존재하지 않습니다.") }

        if (schedule.performancePost.id != request.performancePostId) {
            throw IllegalArgumentException("선택한 회차가 해당 공연에 속하지 않습니다.")
        }

        // 3. 공연 제목 확인 (클라이언트와 서버 데이터 일치 확인)
        if (post.title != request.performanceTitle) {
            throw IllegalArgumentException("공연 정보가 일치하지 않습니다. 페이지를 새로고침해주세요.")
        }

        // 4. 티켓 옵션이 해당 공연에 속하는지 확인
        val ticketOption = ticketOptionRepository.findById(request.ticketOptionId)
            .orElseThrow { NotFoundException("해당 티켓 옵션이 존재하지 않습니다.") }

        if (ticketOption.performancePost.id != request.performancePostId) {
            throw IllegalArgumentException("선택한 티켓 옵션이 해당 공연에 속하지 않습니다.")
        }

        // 5. 가격 검증
        val expectedPrice = ticketOption.price * request.count
        if (expectedPrice != request.expectedTotalPrice) {
            throw IllegalArgumentException("가격 정보가 일치하지 않습니다. 현재 가격: $expectedPrice 원")
        }

        // 6. 예매 가능 여부 검사
        val availability = checkReservationAvailability(user, request.performancePostId)

        if (!availability.isAvailable) {
            throw IllegalArgumentException(availability.message)
        }

        if (request.count > availability.remainingUserQuota) {
            throw IllegalArgumentException("예매 가능 수량을 초과했습니다.")
        }

        // 7. 좌석 선택 검증 (지정석인 경우)
        if (request.selectedSeatCodes.isNotEmpty()) {
            validateSeatSelection(request.selectedSeatCodes, schedule, request.count)
        }

        // 8. 예매 신청 생성
        val reservation = ReservationRequest(
            performanceSchedule = schedule,
            ticketOption = ticketOption,
            user = user,
            count = request.count,
            status = ReservationStatus.PENDING
        )

        val savedReservation = reservationRequestRepository.save(reservation)

        // 9. 좌석 예매 정보 저장 (지정석인 경우)
        if (request.selectedSeatCodes.isNotEmpty()) {
            saveReservationSeats(savedReservation, request.selectedSeatCodes, schedule)
        }

        return ReservationResponse(
            reservationId = savedReservation.id!!,
            performanceTitle = schedule.performancePost.title,
            performanceDate = schedule.startTime,
            ticketOptionName = ticketOption.name,
            ticketPrice = ticketOption.price,
            count = request.count,
            totalPrice = ticketOption.price * request.count,
            status = savedReservation.status,
            selectedSeats = request.selectedSeatCodes,
            regDate = savedReservation.regDate!!
        )
    }

    // === 헬퍼 메서드들 ===
    @Transactional(readOnly = true)
    fun getUserReservations(user: User): List<ReservationResponse> {
        val reservations = reservationRequestRepository.findByUserOrderByRegDateDesc(user)

        return reservations.map { reservation ->
            val reservationSeats = reservationSeatRepository.findByReservationRequest(reservation)
            val seatCodes = reservationSeats.map { it.scheduleSeat.seatCode }

            ReservationResponse(
                reservationId = reservation.id!!,
                performanceTitle = reservation.performanceSchedule.performancePost.title,
                performanceDate = reservation.performanceSchedule.startTime,
                ticketOptionName = reservation.ticketOption.name,
                ticketPrice = reservation.ticketOption.price,
                count = reservation.count,
                totalPrice = reservation.ticketOption.price * reservation.count,
                status = reservation.status,
                selectedSeats = seatCodes,
                regDate = reservation.regDate!!
            )
        }
    }

    // 7. 예매 취소
    @Transactional
    fun cancelReservation(user: User, reservationId: Long) {
        // 예매 신청 조회
        val reservation = reservationRequestRepository.findById(reservationId)
            .orElseThrow { NotFoundException("해당 예매 신청이 존재하지 않습니다.") }

        // 권한 확인: 예매한 본인만 취소 가능
        if (reservation.user.id != user.id) {
            throw IllegalArgumentException("예매 취소 권한이 없습니다.")
        }

        // 취소 가능한 상태 확인 (PENDING만 취소 가능)
        if (reservation.status != ReservationStatus.PENDING) {
            throw IllegalArgumentException("취소할 수 없는 예매 상태입니다. 현재 상태: ${reservation.status}")
        }

        // 예매와 연결된 좌석들 조회
        val reservationSeats = reservationSeatRepository.findByReservationRequest(reservation)

        // 먼저 예매-좌석 연결 정보 삭제 (순서 중요!)
        reservationSeats.forEach { reservationSeat ->
            reservationSeatRepository.delete(reservationSeat)
        }

        // 그 다음 좌석 상태 처리
        reservationSeats.forEach { reservationSeat ->
            val scheduleSeat = reservationSeat.scheduleSeat

            // 좌석이 PENDING 상태였다면 DB에서 삭제 (AVAILABLE 상태로 복원)
            if (scheduleSeat.status == SeatStatus.PENDING) {
                scheduleSeatRepository.delete(scheduleSeat)
            } else {
                // 다른 상태였다면 AVAILABLE로 변경
                scheduleSeat.status = SeatStatus.AVAILABLE
                scheduleSeatRepository.save(scheduleSeat)
            }
        }

        // 마지막에 예매 신청 삭제
        reservationRequestRepository.delete(reservation)
    }

    private fun calculateTotalSeats(schedule: PerformanceSchedule): Int {
        // 실제 구현에서는 좌석 배치에 따라 계산
        // 예시: 10x10 좌석 배치라면 100석
        return 100 // 임시값
    }

    private fun calculateAvailableCount(option: TicketOption, post: PerformancePost): Int {
        // 해당 옵션으로 예매 가능한 수량 계산 로직
        return post.maxTicketsPerUser // 임시값
    }

    private fun getPostIdFromSchedule(scheduleId: Long): Long {
        val schedule = performanceScheduleRepository.findById(scheduleId)
            .orElseThrow { NotFoundException("해당 공연 회차가 존재하지 않습니다.") }
        return schedule.performancePost.id!!
    }

    private fun validateSeatSelection(seatCodes: List<String>, schedule: PerformanceSchedule, count: Int) {
        if (seatCodes.size != count) {
            throw IllegalArgumentException("선택한 좌석 수와 예매 수량이 일치하지 않습니다.")
        }

        // 전체 가능한 좌석 범위 체크
        val validSeatPattern = Regex("^[A-J]([1-9]|10)$")
        val invalidSeats = seatCodes.filter { !validSeatPattern.matches(it) }
        if (invalidSeats.isNotEmpty()) {
            throw IllegalArgumentException("잘못된 좌석 코드입니다: ${invalidSeats.joinToString(", ")}")
        }

        // DB에서 예외 상태 좌석들만 조회 (UNAVAILABLE, RESERVED, PENDING)
        val exceptionSeats = scheduleSeatRepository.findByPerformanceScheduleIn(listOf(schedule))
            .filter { it.status != SeatStatus.AVAILABLE }
            .map { it.seatCode }

        // 선택한 좌석 중 예외 상태인 좌석 확인
        val unavailableSeats = seatCodes.filter { it in exceptionSeats }
        if (unavailableSeats.isNotEmpty()) {
            throw IllegalArgumentException("선택할 수 없는 좌석입니다: ${unavailableSeats.joinToString(", ")}")
        }
    }

    private fun saveReservationSeats(reservation: ReservationRequest, seatCodes: List<String>, schedule: PerformanceSchedule) {
        seatCodes.forEach { seatCode ->
            // 기존에 해당 좌석 데이터가 있는지 확인
            val existingSeat = scheduleSeatRepository.findByPerformanceScheduleIn(listOf(schedule))
                .find { it.seatCode == seatCode }

            if (existingSeat != null) {
                // 기존 좌석 상태를 PENDING으로 업데이트 (입금 확인 전)
                existingSeat.status = SeatStatus.PENDING
                scheduleSeatRepository.save(existingSeat)
            } else {
                // 새 좌석 데이터 생성 (PENDING 상태로 시작)
                val newSeat = ScheduleSeat(
                    seatCode = seatCode,
                    status = SeatStatus.PENDING,  // ← PENDING으로 시작
                    performanceSchedule = schedule
                )
                scheduleSeatRepository.save(newSeat)
            }

            // 예매 좌석 연결 정보 저장
            reservationSeatRepository.save(
                ReservationSeat(
                    reservationRequest = reservation,
                    scheduleSeat = scheduleSeatRepository.findByPerformanceScheduleIn(listOf(schedule))
                        .find { it.seatCode == seatCode }!!
                )
            )
        }
    }
}