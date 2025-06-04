package org.example.camticketkotlin

import org.example.camticketkotlin.domain.*
import org.example.camticketkotlin.domain.enums.*
import org.example.camticketkotlin.dto.UserDto
import org.example.camticketkotlin.dto.request.*
import org.example.camticketkotlin.repository.*
import org.example.camticketkotlin.service.ReservationService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@SpringBootTest
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
])
@Transactional
class ReservationServiceIntegrationTest {

    @Autowired
    private lateinit var reservationService: ReservationService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var performancePostRepository: PerformancePostRepository

    @Autowired
    private lateinit var performanceScheduleRepository: PerformanceScheduleRepository

    @Autowired
    private lateinit var ticketOptionRepository: TicketOptionRepository

    @Autowired
    private lateinit var reservationRequestRepository: ReservationRequestRepository

    @Autowired
    private lateinit var scheduleSeatRepository: ScheduleSeatRepository

    @Autowired
    private lateinit var reservationSeatRepository: ReservationSeatRepository

    private lateinit var testUser: User
    private lateinit var artistUser: User
    private lateinit var testPost: PerformancePost
    private lateinit var testSchedule: PerformanceSchedule
    private lateinit var testTicketOption: TicketOption

    @BeforeEach
    fun setUp() {
        // 예매자 (일반 사용자)
        testUser = createUser("예매자", "customer@test.com", Role.ROLE_USER)
        
        // 공연 등록자 (아티스트)
        artistUser = createUser("아티스트", "artist@test.com", Role.ROLE_MANAGER)
        
        // 공연 게시글 생성
        testPost = createPerformancePost()
        
        // 공연 회차 생성
        testSchedule = createPerformanceSchedule()
        
        // 티켓 옵션 생성
        testTicketOption = createTicketOption("일반", 10000)
    }

    @Test
    @DisplayName("공연 회차 목록 조회 성공")
    fun `getPerformanceSchedules should return schedules with availability info`() {
        // When
        val result = reservationService.getPerformanceSchedules(testPost.id!!)

        // Then
        assertEquals(1, result.size)
        val schedule = result[0]
        assertEquals(testSchedule.id, schedule.scheduleId)
        assertEquals(testSchedule.startTime, schedule.startTime)
        assertTrue(schedule.isBookingAvailable)
        assertEquals(100, schedule.totalSeats)  // 기본 좌석 수
        assertEquals(100, schedule.availableSeats)  // 예매 전이므로 전체 좌석 사용 가능
    }

    @Test
    @DisplayName("티켓 옵션 조회 성공")
    fun `getTicketOptions should return available ticket options`() {
        // When
        val result = reservationService.getTicketOptions(testPost.id!!)

        // Then
        assertEquals(1, result.size)
        val option = result[0]
        assertEquals(testTicketOption.id, option.optionId)
        assertEquals("일반", option.name)
        assertEquals(10000, option.price)
        assertTrue(option.availableCount > 0)
    }

    @Test
    @DisplayName("예매 가능 여부 확인 - 가능한 경우")
    fun `checkReservationAvailability should return available when conditions met`() {
        // When
        val result = reservationService.checkReservationAvailability(testUser, testPost.id!!)

        // Then
        assertTrue(result.isAvailable)
        assertEquals(4, result.maxReservableCount)  // maxTicketsPerUser
        assertEquals(0, result.currentUserReservationCount)
        assertEquals(4, result.remainingUserQuota)
        assertEquals("예매 가능합니다.", result.message)
    }

    @Test
    @DisplayName("예매 가능 여부 확인 - 예매 기간 아님")
    fun `checkReservationAvailability should return false when not in reservation period`() {
        // Given - 예매 기간이 지난 공연 생성
        val pastPost = createPerformancePost(
            reservationStartAt = LocalDateTime.now().minusDays(10),
            reservationEndAt = LocalDateTime.now().minusDays(1)
        )

        // When
        val result = reservationService.checkReservationAvailability(testUser, pastPost.id!!)

        // Then
        assertFalse(result.isAvailable)
        assertEquals("예매 마감되었습니다.", result.message)
    }

    @Test
    @DisplayName("예매 신청 성공 - 자유석")
    fun `createReservation should create reservation successfully for free seating`() {
        // Given
        val request = ReservationCreateRequest(
            performancePostId = testPost.id!!,
            performanceScheduleId = testSchedule.id!!,
            selectedSeatCodes = emptyList(),  // 자유석이므로 빈 리스트
            isPaymentCompleted = true,
            userBankAccount = "1111-2222-3333",
            ticketOrders = listOf(
                TicketOrderItem(
                    ticketOptionId = testTicketOption.id!!,
                    count = 2,
                    unitPrice = 10000
                )
            )
        )

        // When
        val result = reservationService.createReservation(testUser, request)

        // Then
        assertNotNull(result)
        assertEquals(testSchedule.performancePost.title, result.performanceTitle)
        assertEquals(testSchedule.startTime, result.performanceDate)
        assertEquals(2, result.count)
        assertEquals(20000, result.totalPrice)
        assertEquals(ReservationStatus.PENDING, result.status)

        // 데이터베이스 검증
        val savedReservation = reservationRequestRepository.findById(result.reservationId).get()
        assertEquals(testUser.id, savedReservation.user.id)
        assertEquals(testSchedule.id, savedReservation.performanceSchedule.id)
        assertEquals(testTicketOption.id, savedReservation.ticketOption.id)
        assertEquals(2, savedReservation.count)
    }

    @Test
    @DisplayName("예매 신청 성공 - 지정석")
    fun `createReservation should create reservation successfully for assigned seating`() {
        // Given - 지정석 공연으로 변경
        testPost.ticketType = TicketType.PAID
        performancePostRepository.save(testPost)

        val request = ReservationCreateRequest(
            performancePostId = testPost.id!!,
            performanceScheduleId = testSchedule.id!!,
            selectedSeatCodes = listOf("A1", "A2"),
            isPaymentCompleted = true,
            userBankAccount = "1111-2222-3333",
            ticketOrders = listOf(
                TicketOrderItem(
                    ticketOptionId = testTicketOption.id!!,
                    count = 2,
                    unitPrice = 10000
                )
            )
        )

        // When
        val result = reservationService.createReservation(testUser, request)

        // Then
        assertNotNull(result)
        assertEquals(listOf("A1", "A2"), result.selectedSeats)

        // 좌석 상태 확인
        val seats = scheduleSeatRepository.findByPerformanceScheduleIn(listOf(testSchedule))
        val reservedSeats = seats.filter { it.seatCode in listOf("A1", "A2") }
        assertEquals(2, reservedSeats.size)
        assertTrue(reservedSeats.all { it.status == SeatStatus.PENDING })
    }

    @Test
    @DisplayName("예매 신청 실패 - 수량 초과")
    fun `createReservation should fail when exceeding user quota`() {
        // Given - 사용자 한도 초과 요청
        val request = ReservationCreateRequest(
            performancePostId = testPost.id!!,
            performanceScheduleId = testSchedule.id!!,
            selectedSeatCodes = emptyList(),
            isPaymentCompleted = true,
            userBankAccount = "1111-2222-3333",
            ticketOrders = listOf(
                TicketOrderItem(
                    ticketOptionId = testTicketOption.id!!,
                    count = 5,  // maxTicketsPerUser(4)보다 많음
                    unitPrice = 10000
                )
            )
        )

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            reservationService.createReservation(testUser, request)
        }

        assertTrue(exception.message!!.contains("예매 가능 수량을 초과"))
    }

    @Test
    @DisplayName("사용자 예매 내역 조회")
    fun `getUserReservations should return user reservations`() {
        // Given - 예매 생성
        createTestReservation()

        // When
        val result = reservationService.getUserReservations(testUser)

        // Then
        assertEquals(1, result.size)
        val reservation = result[0]
        assertEquals(testPost.title, reservation.performanceTitle)
        assertEquals(testUser.id, testUser.id)  // 사용자 확인
        assertEquals(ReservationStatus.PENDING, reservation.status)
    }

    @Test
    @DisplayName("예매 상태 변경 - 승인")
    fun `updateReservationStatus should approve reservation`() {
        // Given
        val reservation = createTestReservation()

        // When
        reservationService.updateReservationStatus(artistUser, reservation.id!!, "APPROVED")

        // Then
        val updatedReservation = reservationRequestRepository.findById(reservation.id!!).get()
        assertEquals(ReservationStatus.APPROVED, updatedReservation.status)
    }

    @Test
    @DisplayName("예매 상태 변경 - 권한 없음")
    fun `updateReservationStatus should fail when no permission`() {
        // Given
        val reservation = createTestReservation()
        val otherUser = createUser("다른사용자", "other@test.com", Role.ROLE_MANAGER)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            reservationService.updateReservationStatus(otherUser, reservation.id!!, "APPROVED")
        }

        assertEquals("예매 상태 변경 권한이 없습니다.", exception.message)
    }

    @Test
    @DisplayName("예매 취소 성공")
    fun `cancelReservation should cancel pending reservation`() {
        // Given
        val reservation = createTestReservation()

        // When
        reservationService.cancelReservation(testUser, reservation.id!!)

        // Then
        val deletedReservation = reservationRequestRepository.findById(reservation.id!!)
        assertTrue(deletedReservation.isEmpty, "예매가 삭제되어야 함")
    }

    @Test
    @DisplayName("예매 취소 실패 - 이미 승인된 예매")
    fun `cancelReservation should fail when reservation is already approved`() {
        // Given
        val reservation = createTestReservation()
        reservation.status = ReservationStatus.APPROVED
        reservationRequestRepository.save(reservation)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            reservationService.cancelReservation(testUser, reservation.id!!)
        }

        assertTrue(exception.message!!.contains("취소할 수 없는 예매 상태"))
    }

    @Test
    @DisplayName("환불 신청 성공")
    fun `requestRefund should create refund request`() {
        // Given - 승인된 예매
        val reservation = createTestReservation()
        reservation.status = ReservationStatus.APPROVED
        reservationRequestRepository.save(reservation)

        // When
        val result = reservationService.requestRefund(testUser, reservation.id!!)

        // Then
        assertNotNull(result)
        assertEquals(reservation.id, result.reservationId)
        assertEquals(testPost.title, result.performanceTitle)
        assertEquals(ReservationStatus.REFUND_REQUESTED, result.status)

        // 데이터베이스 확인
        val updatedReservation = reservationRequestRepository.findById(reservation.id!!).get()
        assertEquals(ReservationStatus.REFUND_REQUESTED, updatedReservation.status)
    }

    @Test
    @DisplayName("환불 처리 - 승인")
    fun `processRefund should approve refund`() {
        // Given - 환불 신청된 예매
        val reservation = createTestReservation()
        reservation.status = ReservationStatus.REFUND_REQUESTED
        reservationRequestRepository.save(reservation)

        // When
        reservationService.processRefund(artistUser, reservation.id!!, true)

        // Then
        val processedReservation = reservationRequestRepository.findById(reservation.id!!).get()
        assertEquals(ReservationStatus.REFUNDED, processedReservation.status)
    }

    @Test
    @DisplayName("관리자 예매 신청 목록 조회")
    fun `getReservationRequestsForMyPerformances should return artist reservations`() {
        // Given
        createTestReservation()

        // When
        val result = reservationService.getReservationRequestsForMyPerformances(artistUser)

        // Then
        assertEquals(1, result.size)
        val reservationMgmt = result[0]
        assertEquals(testPost.title, reservationMgmt.performanceTitle)
        assertEquals(testUser.nickName, reservationMgmt.userNickName)
        assertEquals(ReservationStatus.PENDING, reservationMgmt.status)
    }

    @Test
    @DisplayName("예매 상세 정보 조회 - 사용자용")
    fun `getReservationDetail should return detailed reservation info`() {
        // Given
        val reservation = createTestReservation()

        // When
        val result = reservationService.getReservationDetail(testUser, reservation.id!!)

        // Then
        assertNotNull(result)
        assertEquals(reservation.id, result.reservationId)
        assertEquals(testPost.title, result.performanceInfo.title)
        assertEquals(testUser.nickName, result.reservationInfo.userNickName)
        assertEquals(ReservationStatus.PENDING, result.status)
    }

    // === 헬퍼 메서드들 ===

    private fun createUser(name: String, email: String, role: Role): User {
        val user = User.from(UserDto(
            kakaoId = System.currentTimeMillis(),  // 유니크한 ID
            name = name,
            email = email,
            profileImageUrl = "test.jpg",
            role = role
        ))
        user.nickName = "${name}닉네임"
        return userRepository.save(user)
    }

    private fun createPerformancePost(
        reservationStartAt: LocalDateTime = LocalDateTime.now().minusHours(1),
        reservationEndAt: LocalDateTime = LocalDateTime.now().plusDays(7)
    ): PerformancePost {
        val post = PerformancePost(
            title = "테스트 공연",
            category = PerformanceCategory.CONCERT,
            location = PerformanceLocation.HAKGWAN_104,
            ticketType = TicketType.FREE,  // 기본은 자유석
            maxTicketsPerUser = 4,
            backAccount = "1234-5678-9012",
            reservationStartAt = reservationStartAt,
            reservationEndAt = reservationEndAt,
            timeNotice = "시간 안내",
            priceNotice = "가격 안내",
            reservationNotice = "예매 안내",
            profileImageUrl = "https://test.com/profile.jpg",
            user = artistUser
        )
        return performancePostRepository.save(post)
    }

    private fun createPerformanceSchedule(): PerformanceSchedule {
        val schedule = PerformanceSchedule(
            startTime = LocalDateTime.now().plusDays(1),
            performancePost = testPost
        )
        return performanceScheduleRepository.save(schedule)
    }

    private fun createTicketOption(name: String, price: Int): TicketOption {
        val option = TicketOption(
            name = name,
            price = price,
            performancePost = testPost
        )
        return ticketOptionRepository.save(option)
    }

    private fun createTestReservation(): ReservationRequest {
        val reservation = ReservationRequest(
            performanceSchedule = testSchedule,
            ticketOption = testTicketOption,
            user = testUser,
            count = 2,
            status = ReservationStatus.PENDING
        )
        return reservationRequestRepository.save(reservation)
    }
}