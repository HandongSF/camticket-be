package org.example.camticketkotlin.service

import AdminReservationDetailResponse
import org.example.camticketkotlin.domain.*
import org.example.camticketkotlin.dto.request.RefundCreateRequest
import org.example.camticketkotlin.dto.request.ReservationCreateRequest
import org.example.camticketkotlin.dto.response.*
import org.example.camticketkotlin.exception.NotFoundException
import org.example.camticketkotlin.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.example.camticketkotlin.domain.enums.TicketType  // 이 줄 추가
import java.time.LocalDateTime

data class Tuple5<T1, T2, T3, T4, T5>(
    val first: T1,
    val second: T2,
    val third: T3,
    val fourth: T4,
    val fifth: T5
)
@Service
@Transactional
class ReservationService(
    private val reservationRequestRepository: ReservationRequestRepository,
    private val reservationSeatRepository: ReservationSeatRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val performancePostRepository: PerformancePostRepository,
    private val ticketOptionRepository: TicketOptionRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val seatLockingDomainService: org.example.camticketkotlin.domain.service.SeatLockingDomainService
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
                // DDD: 도메인 모델의 비즈니스 로직 사용
                isBookingAvailable = availableSeats > 0 && post.isReservationOpen()
            )
        }
    }

    // 2. 좌석 정보 조회 (지정석인 경우) - 예외 상태만 반환
    // READ_COMMITTED 격리 수준으로 다른 트랜잭션의 커밋된 PENDING 좌석도 즉시 확인
    @Transactional(readOnly = true)
    fun getSeatInfo(scheduleId: Long): List<SeatInfoResponse> {
        val schedule = performanceScheduleRepository.findById(scheduleId)
            .orElseThrow { NotFoundException("해당 공연 회차가 존재하지 않습니다.") }

        // 지정석이 아닌 경우 빈 리스트 반환
        if (schedule.performancePost.ticketType.name != "PAID") {
            return emptyList()
        }

        // DB에서 예외 상태 좌석들만 조회하여 반환
        // 커밋된 PENDING 상태 좌석도 다른 사용자에게 "선택 불가"로 보임
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

        // 4. 티켓 주문 검증
        if (request.ticketOrders.isEmpty()) {
            throw IllegalArgumentException("티켓 주문 정보가 없습니다.")
        }

        var totalTicketCount = 0
        var totalPrice = 0

        // 5. 먼저 수량과 가격만 계산 및 검증 (저장 안함)
        request.ticketOrders.forEach { ticketOrder ->
            // 티켓 옵션 검증
            val ticketOption = ticketOptionRepository.findById(ticketOrder.ticketOptionId)
                .orElseThrow { NotFoundException("해당 티켓 옵션이 존재하지 않습니다.") }

            if (ticketOption.performancePost.id != request.performancePostId) {
                throw IllegalArgumentException("선택한 티켓 옵션이 해당 공연에 속하지 않습니다.")
            }

            // 가격 검증
            if (ticketOption.price != ticketOrder.unitPrice) {
                throw IllegalArgumentException("가격 정보가 일치하지 않습니다. ${ticketOption.name}: 현재 가격 ${ticketOption.price}원")
            }

            totalTicketCount += ticketOrder.count
            totalPrice += ticketOption.price * ticketOrder.count
        }

        // 6. 예매 가능 여부 검사 (저장 전에 체크!)
        val availability = checkReservationAvailability(user, request.performancePostId)

        if (!availability.isAvailable) {
            throw IllegalArgumentException(availability.message)
        }

        if (totalTicketCount > availability.remainingUserQuota) {
//            println("🚨 수량 초과 에러!")
//            println("   - 요청 수량: $totalTicketCount")
//            println("   - 남은 할당량: ${availability.remainingUserQuota}")
            throw IllegalArgumentException("예매 가능 수량을 초과했습니다.")
        }

        // 7. 좌석 먼저 선점 (지정석인 경우) - Domain Service 호출 (별도 트랜잭션으로 즉시 커밋!)
        val lockedSeats = if (request.selectedSeatCodes.isNotEmpty()) {
            if (request.selectedSeatCodes.size != totalTicketCount) {
                throw IllegalArgumentException("선택한 좌석 수와 티켓 수량이 일치하지 않습니다.")
            }
            // DDD: Domain Service를 통한 좌석 선점 (즉시 커밋됨 → 다른 사용자에게 즉시 PENDING으로 보임)
            seatLockingDomainService.lockSeats(request.selectedSeatCodes, schedule)
        } else {
            emptyList()
        }

        // 8. 이제 실제로 예매 신청 저장
        val savedReservations = mutableListOf<ReservationRequest>()
        request.ticketOrders.forEach { ticketOrder ->
            val ticketOption = ticketOptionRepository.findById(ticketOrder.ticketOptionId).get()

            // 예매 신청 생성
            val reservation = ReservationRequest(
                performanceSchedule = schedule,
                ticketOption = ticketOption,
                user = user,
                count = ticketOrder.count,
                status = ReservationStatus.PENDING
            )

            savedReservations.add(reservationRequestRepository.save(reservation))
        }

        // 9. 좌석-예매 연결 (지정석인 경우)
        if (lockedSeats.isNotEmpty() && savedReservations.isNotEmpty()) {
            linkSeatsToReservation(savedReservations.first(), lockedSeats)
        }

        // 10. 응답 생성 (첫 번째 예매 기준으로 반환)
        val mainReservation = savedReservations.first()
        val allTicketOptions = savedReservations.map { "${it.ticketOption.name}(${it.count}매)" }.joinToString(", ")

        return ReservationResponse(
            reservationId = mainReservation.id!!,
            performanceTitle = schedule.performancePost.title,
            performanceDate = schedule.startTime,
            ticketOptionName = allTicketOptions,
            ticketPrice = totalPrice / totalTicketCount, // 평균 단가
            count = totalTicketCount,
            totalPrice = totalPrice,
            status = mainReservation.status,
            selectedSeats = request.selectedSeatCodes,
            regDate = mainReservation.regDate!!
        )
    }

    // 8. 내 공연의 예매 신청 목록 조회 (관리자용)
    @Transactional(readOnly = true)
    fun getReservationRequestsForMyPerformances(user: User): List<ReservationManagementResponse> {
        // 내가 등록한 공연들 조회
        val myPosts = performancePostRepository.findAllByUserId(user.id!!)

        if (myPosts.isEmpty()) {
            return emptyList()
        }

        // 내 공연들의 모든 회차 조회
        val mySchedules = myPosts.flatMap { post ->
            performanceScheduleRepository.findByPerformancePost(post)
        }

        // 해당 회차들에 대한 모든 예매 신청 조회
        val reservations = mySchedules.flatMap { schedule ->
            reservationRequestRepository.findByPerformanceScheduleIdOrderByRegDateDesc(schedule.id!!)
        }

        return reservations.map { reservation ->
            val reservationSeats = reservationSeatRepository.findByReservationRequest(reservation)
            val seatCodes = reservationSeats.map { it.scheduleSeat.seatCode }

            ReservationManagementResponse(
                reservationId = reservation.id!!,
                performanceTitle = reservation.performanceSchedule.performancePost.title,
                performanceDate = reservation.performanceSchedule.startTime,
                userNickName = reservation.user.nickName ?: "Unknown",
                userEmail = reservation.user.email ?: "Unknown",
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

    // 9. 예매 상태 변경 (관리자용)
    @Transactional
    fun updateReservationStatus(user: User, reservationId: Long, newStatus: String) {
        // 예매 신청 조회
        val reservation = reservationRequestRepository.findById(reservationId)
            .orElseThrow { NotFoundException("해당 예매 신청이 존재하지 않습니다.") }

        // 권한 확인: 해당 공연을 등록한 사람만 상태 변경 가능
        val performanceOwner = reservation.performanceSchedule.performancePost.user
        require(performanceOwner.isOwnedBy(user.id!!)) { "예매 상태 변경 권한이 없습니다." }

        // 상태 변환 및 유효성 검사
        val targetStatus = try {
            ReservationStatus.valueOf(newStatus.uppercase())
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("잘못된 예매 상태입니다: $newStatus")
        }

        // DDD: 도메인 로직 사용하여 상태 변경
        when (targetStatus) {
            ReservationStatus.APPROVED -> {
                // 승인 시: 도메인 메서드 호출 후 좌석 상태 업데이트
                reservation.approve()
                reservationRequestRepository.save(reservation)
                // 좌석을 RESERVED로 변경
                val seats = reservationSeatRepository.findByReservationRequest(reservation)
                    .map { it.scheduleSeat }
                seatLockingDomainService.confirmReservation(seats)
            }
            ReservationStatus.REJECTED -> {
                // 거절 시: 도메인 메서드 호출 후 좌석 해제
                reservation.reject()
                reservationRequestRepository.save(reservation)
                // 좌석을 다시 AVAILABLE로 복원 (PENDING 데이터 삭제)
                val seats = reservationSeatRepository.findByReservationRequest(reservation)
                    .map { it.scheduleSeat }
                seatLockingDomainService.releaseSeats(seats)
            }
            else -> {
                throw IllegalArgumentException("APPROVED 또는 REJECTED 상태로만 변경 가능합니다.")
            }
        }
    }

    // 10. 특정 공연의 예매 신청 목록 조회 (관리자용)
    @Transactional(readOnly = true)
    fun getReservationRequestsForPerformance(user: User, postId: Long): List<ReservationManagementResponse> {
        // 공연 조회 및 권한 확인
        val post = performancePostRepository.findById(postId)
            .orElseThrow { NotFoundException("해당 공연이 존재하지 않습니다.") }

        if (post.user.id != user.id) {
            throw IllegalArgumentException("해당 공연의 예매 현황을 조회할 권한이 없습니다.")
        }

        // 해당 공연의 모든 회차 조회
        val schedules = performanceScheduleRepository.findByPerformancePost(post)

        // 모든 회차의 예매 신청 조회
        val reservations = schedules.flatMap { schedule ->
            reservationRequestRepository.findByPerformanceScheduleIdOrderByRegDateDesc(schedule.id!!)
        }

        return reservations.map { reservation ->
            val reservationSeats = reservationSeatRepository.findByReservationRequest(reservation)
            val seatCodes = reservationSeats.map { it.scheduleSeat.seatCode }

            ReservationManagementResponse(
                reservationId = reservation.id!!,
                performanceTitle = reservation.performanceSchedule.performancePost.title,
                performanceDate = reservation.performanceSchedule.startTime,
                userNickName = reservation.user.nickName ?: "Unknown",
                userEmail = reservation.user.email ?: "Unknown",
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

    // 헬퍼 메서드: 좌석 상태 업데이트
    private fun updateSeatsStatus(reservation: ReservationRequest, newStatus: SeatStatus) {
        val reservationSeats = reservationSeatRepository.findByReservationRequest(reservation)
        reservationSeats.forEach { reservationSeat ->
            val scheduleSeat = reservationSeat.scheduleSeat
            scheduleSeat.status = newStatus
            scheduleSeatRepository.save(scheduleSeat)
        }
    }

    // 헬퍼 메서드: 좌석 해제 (AVAILABLE로 복원)
    private fun releaseSeats(reservation: ReservationRequest) {
        val reservationSeats = reservationSeatRepository.findByReservationRequest(reservation)

        // 1단계: ReservationSeat 연결 먼저 삭제
        reservationSeats.forEach { reservationSeat ->
            reservationSeatRepository.delete(reservationSeat)
        }

        // 2단계: 그 다음 ScheduleSeat 처리
        reservationSeats.forEach { reservationSeat ->
            val scheduleSeat = reservationSeat.scheduleSeat
            if (scheduleSeat.status == SeatStatus.PENDING) {
                scheduleSeatRepository.delete(scheduleSeat)
            }
        }
    }
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

        // 일반 조회로 변경 (데드락 방지)
        // 실제 락은 saveReservationSeats에서만 걸어서 데드락 최소화
        seatCodes.forEach { seatCode ->
            val existingSeat = scheduleSeatRepository.findByPerformanceScheduleAndSeatCode(schedule, seatCode)

            // 좌석이 이미 존재하고 예매 불가 상태인 경우
            if (existingSeat != null &&
                (existingSeat.status == SeatStatus.PENDING ||
                 existingSeat.status == SeatStatus.RESERVED ||
                 existingSeat.status == SeatStatus.UNAVAILABLE)) {
                throw IllegalArgumentException("선택할 수 없는 좌석입니다: $seatCode (${existingSeat.status})")
            }
        }
    }

    // 좌석 선점 (별도 트랜잭션으로 즉시 커밋)
    // Gap Lock 데드락 방지를 위해 SELECT FOR UPDATE 제거, INSERT-first 전략 사용
    //
    // DDD: 이 메서드는 더 이상 직접 사용되지 않습니다.
    // 대신 SeatLockingDomainService.lockSeats()를 사용하세요.
    @Deprecated("Use SeatLockingDomainService.lockSeats() instead")
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    fun lockSeatsImmediately(seatCodes: List<String>, schedule: PerformanceSchedule): List<ScheduleSeat> {
        // DDD: Domain Service로 위임
        return seatLockingDomainService.lockSeats(seatCodes, schedule)
    }

    // 예매-좌석 연결 (메인 트랜잭션에서 호출)
    private fun linkSeatsToReservation(reservation: ReservationRequest, seats: List<ScheduleSeat>) {
        seats.forEach { scheduleSeat ->
            // DDD: 팩토리 메서드를 사용하여 예매-좌석 연결 생성 (도메인 검증 포함)
            reservationSeatRepository.save(
                ReservationSeat.create(reservation, scheduleSeat)
            )
        }
    }


    // 환불 신청 (사유 없이 간단하게)
    @Transactional
    fun requestRefund(user: User, reservationId: Long): RefundResponse {
        // 예매 신청 조회
        val reservation = reservationRequestRepository.findById(reservationId)
            .orElseThrow { NotFoundException("해당 예매 신청이 존재하지 않습니다.") }

        // 권한 확인: 예매한 본인만 환불 신청 가능
        if (reservation.user.id != user.id) {
            throw IllegalArgumentException("환불 신청 권한이 없습니다.")
        }

        // 환불 신청 가능한 상태 확인 (APPROVED만 환불 신청 가능)
        if (reservation.status != ReservationStatus.APPROVED) {
            throw IllegalArgumentException("승인된 예매만 환불 신청할 수 있습니다. 현재 상태: ${reservation.status}")
        }

        // 공연 날짜 확인 (공연 후에는 환불 불가 - 옵션)
        if (reservation.performanceSchedule.startTime.isBefore(LocalDateTime.now())) {
            throw IllegalArgumentException("공연이 이미 시작되어 환불 신청할 수 없습니다.")
        }

        // 상태를 REFUND_REQ로 변경
        reservation.status = ReservationStatus.REFUND_REQUESTED
        reservationRequestRepository.save(reservation)

        return RefundResponse(
            reservationId = reservation.id!!,
            performanceTitle = reservation.performanceSchedule.performancePost.title,
            performanceDate = reservation.performanceSchedule.startTime,
            ticketOptionName = reservation.ticketOption.name,
            totalPrice = reservation.ticketOption.price * reservation.count,
            status = reservation.status,
            requestDate = LocalDateTime.now()
        )
    }

    // 환불 승인/거절 (관리자용)
    @Transactional
    fun processRefund(user: User, reservationId: Long, approve: Boolean): Unit {
        // 예매 신청 조회
        val reservation = reservationRequestRepository.findById(reservationId)
            .orElseThrow { NotFoundException("해당 예매 신청이 존재하지 않습니다.") }

        // 권한 확인: 해당 공연을 등록한 사람만 환불 처리 가능
        val performanceOwner = reservation.performanceSchedule.performancePost.user
        if (performanceOwner.id != user.id) {
            throw IllegalArgumentException("환불 처리 권한이 없습니다.")
        }

        // 환불 신청 상태인지 확인
        if (reservation.status != ReservationStatus.REFUND_REQUESTED) {
            throw IllegalArgumentException("환불 신청 상태가 아닙니다. 현재 상태: ${reservation.status}")
        }

        if (approve) {
            // 환불 승인: 상태 변경 + 좌석 해제
            reservation.status = ReservationStatus.REFUNDED
            reservationRequestRepository.save(reservation)

            // 좌석을 다시 AVAILABLE로 변경 (예매 가능하도록)
            releaseSeatsForRefund(reservation)
        } else {
            // 환불 거절: 다시 APPROVED 상태로 복원
            reservation.status = ReservationStatus.APPROVED
            reservationRequestRepository.save(reservation)
        }
    }

    // 환불 신청 목록 조회 (관리자용)
    @Transactional(readOnly = true)
    fun getRefundRequests(user: User): List<ReservationManagementResponse> {
        // 내가 등록한 공연들 조회
        val myPosts = performancePostRepository.findAllByUserId(user.id!!)

        if (myPosts.isEmpty()) {
            return emptyList()
        }

        // 내 공연들의 모든 회차 조회
        val mySchedules = myPosts.flatMap { post ->
            performanceScheduleRepository.findByPerformancePost(post)
        }

        // 환불 신청 상태인 예매들만 조회
        val refundRequests = mySchedules.flatMap { schedule ->
            reservationRequestRepository.findByPerformanceScheduleIdAndStatusOrderByRegDateDesc(
                schedule.id!!,
                ReservationStatus.REFUND_REQUESTED
            )
        }

        return refundRequests.map { reservation ->
            val reservationSeats = reservationSeatRepository.findByReservationRequest(reservation)
            val seatCodes = reservationSeats.map { it.scheduleSeat.seatCode }

            ReservationManagementResponse(
                reservationId = reservation.id!!,
                performanceTitle = reservation.performanceSchedule.performancePost.title,
                performanceDate = reservation.performanceSchedule.startTime,
                userNickName = reservation.user.nickName ?: "Unknown",
                userEmail = reservation.user.email ?: "Unknown",
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

    // 헬퍼 메서드: 환불용 좌석 해제 (순서 중요!)
    private fun releaseSeatsForRefund(reservation: ReservationRequest) {
        val reservationSeats = reservationSeatRepository.findByReservationRequest(reservation)

        // 1단계: ReservationSeat 연결 먼저 삭제
        reservationSeats.forEach { reservationSeat ->
            reservationSeatRepository.delete(reservationSeat)
        }

        // 2단계: 그 다음 ScheduleSeat 처리
        reservationSeats.forEach { reservationSeat ->
            val scheduleSeat = reservationSeat.scheduleSeat
            // RESERVED 상태 좌석을 DB에서 삭제 (AVAILABLE로 복원)
            if (scheduleSeat.status == SeatStatus.RESERVED) {
                scheduleSeatRepository.delete(scheduleSeat)
            }
        }
    }

    @Transactional(readOnly = true)
    fun getUserReservationsOverview(user: User): List<UserReservationOverviewResponse> {
        val reservations = reservationRequestRepository.findByUserOrderByRegDateDesc(user)

        return reservations.map { reservation ->
            val schedule = reservation.performanceSchedule
            val post = schedule.performancePost
            val artist = post.user  // 🎭 공연 올린 아티스트

            // 좌석 정보 조회
            val reservationSeats = reservationSeatRepository.findByReservationRequest(reservation)
            val seatCodes = reservationSeats.map { it.scheduleSeat.seatCode }

            UserReservationOverviewResponse(
                reservationId = reservation.id!!,
                performanceTitle = post.title,
                performanceDate = schedule.startTime,
                ticketOptionName = reservation.ticketOption.name,
                ticketPrice = reservation.ticketOption.price,
                count = reservation.count,
                totalPrice = reservation.ticketOption.price * reservation.count,
                status = reservation.status,
                selectedSeats = seatCodes,
                regDate = reservation.regDate!!,

                // ✅ UI에 필요한 추가 정보들
                reservationStartAt = post.reservationStartAt,
                reservationEndAt = post.reservationEndAt,
                location = post.location,
                locationDisplayName = post.location.displayName,
                performanceProfileImageUrl = post.profileImageUrl,
                artistId = artist.id!!,
                artistName = artist.name,
                artistProfileImageUrl = artist.profileImageUrl
            )
        }
    }

    @Transactional(readOnly = true)
    fun getReservationDetail(user: User, reservationId: Long): ReservationDetailResponse {
        val mainReservation = reservationRequestRepository.findById(reservationId)
            .orElseThrow { NotFoundException("해당 예매 신청이 존재하지 않습니다.") }

        // 권한 확인
        if (mainReservation.user.id != user.id) {
            throw IllegalArgumentException("예매 상세 정보 조회 권한이 없습니다.")
        }

        val schedule = mainReservation.performanceSchedule
        val post = schedule.performancePost

        // 같은 사용자, 같은 회차의 모든 예매 조회
        val allReservations = reservationRequestRepository
            .findByUserAndPerformanceScheduleOrderByRegDateDesc(user, schedule)

        val allSeatCodes = allReservations.flatMap { reservation ->
            reservationSeatRepository.findByReservationRequest(reservation)
                .map { it.scheduleSeat.seatCode }
        }

        // 회차 정보 계산
        val allSchedules = performanceScheduleRepository.findByPerformancePost(post)
            .sortedBy { it.startTime }
        val scheduleIndex = allSchedules.indexOfFirst { it.id == schedule.id } + 1

        // 총 수량과 가격 계산
        val totalCount = allReservations.sumOf { it.count }
        val totalPrice = allReservations.sumOf { it.ticketOption.price * it.count }

        // PaymentInfo를 배열로 변경
        val paymentInfoList = allReservations.map { reservation ->
            ReservationDetailResponse.PaymentInfoItem(
                ticketOptionName = reservation.ticketOption.name,
                unitPrice = reservation.ticketOption.price,
                quantity = reservation.count,
                subtotal = reservation.ticketOption.price * reservation.count
            )
        }

        return ReservationDetailResponse(
            reservationId = mainReservation.id!!,
            status = mainReservation.status,

            performanceInfo = ReservationDetailResponse.PerformanceInfo(
                title = post.title,
                category = post.category,
                location = post.location,
                performanceDate = schedule.startTime,
                scheduleId = schedule.id!!,
                scheduleIndex = scheduleIndex,
                profileImageUrl = post.profileImageUrl
            ),

            reservationInfo = ReservationDetailResponse.ReservationInfo(
                userNickName = mainReservation.user.nickName ?: "Unknown",
                userBankAccount = mainReservation.user.bankAccount ?: "등록된 계좌 없음",
                ticketCount = totalCount,  // 총 수량
                totalPrice = totalPrice,   // 총 가격
                isPaymentCompleted = false
            ),

            seatInfo = ReservationDetailResponse.SeatInfo(
                ticketType = post.ticketType,
                selectedSeats = allSeatCodes
            ),

            paymentInfo = paymentInfoList,  // 배열로 변경

            reservationDate = mainReservation.regDate!!
        )
    }

    // 관리자용 예매 상세 조회
    @Transactional(readOnly = true)
    fun getAdminReservationDetail(user: User, reservationId: Long): AdminReservationDetailResponse {
        val mainReservation = reservationRequestRepository.findById(reservationId)
            .orElseThrow { NotFoundException("해당 예매 신청이 존재하지 않습니다.") }

        // 권한 확인: 해당 공연을 등록한 사람만 조회 가능
        val performanceOwner = mainReservation.performanceSchedule.performancePost.user
        if (performanceOwner.id != user.id) {
            throw IllegalArgumentException("예매 관리 권한이 없습니다.")
        }

        val schedule = mainReservation.performanceSchedule
        val post = schedule.performancePost

        // 같은 사용자, 같은 회차의 모든 예매 조회
        val allReservations = reservationRequestRepository
            .findByUserAndPerformanceScheduleOrderByRegDateDesc(mainReservation.user, schedule)

        val allSeatCodes = allReservations.flatMap { reservation ->
            reservationSeatRepository.findByReservationRequest(reservation)
                .map { it.scheduleSeat.seatCode }
        }

        // 회차 정보 계산
        val allSchedules = performanceScheduleRepository.findByPerformancePost(post)
            .sortedBy { it.startTime }
        val scheduleIndex = allSchedules.indexOfFirst { it.id == schedule.id } + 1

        // PaymentInfo를 배열로 변경
        val paymentInfoList = allReservations.map { reservation ->
            ReservationDetailResponse.PaymentInfoItem(
                ticketOptionName = reservation.ticketOption.name,
                unitPrice = reservation.ticketOption.price,
                quantity = reservation.count,
                subtotal = reservation.ticketOption.price * reservation.count
            )
        }

        return AdminReservationDetailResponse(
            reservationId = mainReservation.id!!,
            status = mainReservation.status,
            statusDescription = when(mainReservation.status) {
                ReservationStatus.PENDING -> "예매 승인 대기 중"
                ReservationStatus.APPROVED -> "예매 승인 완료"
                ReservationStatus.REJECTED -> "예매 거절됨"
                ReservationStatus.REFUND_REQUESTED -> "환불 신청 접수"
                ReservationStatus.REFUNDED -> "환불 처리 완료"
            },

            performanceInfo = AdminReservationDetailResponse.AdminPerformanceInfo(
                performanceId = post.id!!,
                title = post.title,
                category = post.category,
                location = post.location,
                performanceDate = schedule.startTime,
                scheduleId = schedule.id!!,
                scheduleIndex = scheduleIndex,
                profileImageUrl = post.profileImageUrl
            ),

            customerInfo = AdminReservationDetailResponse.CustomerInfo(
                userId = mainReservation.user.id!!,
                userName = mainReservation.user.name ?: "Unknown",
                userEmail = mainReservation.user.email ?: "Unknown",
                userNickname = mainReservation.user.nickName,
                userBankAccount = mainReservation.user.bankAccount ?: "등록된 계좌 없음"
            ),

            seatInfo = ReservationDetailResponse.SeatInfo(
                ticketType = post.ticketType,
                selectedSeats = allSeatCodes
            ),

            paymentInfo = paymentInfoList
        )
    }

}




