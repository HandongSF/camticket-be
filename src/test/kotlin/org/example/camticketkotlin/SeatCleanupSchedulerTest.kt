package org.example.camticketkotlin

import jakarta.persistence.EntityManager
import org.example.camticketkotlin.domain.*
import org.example.camticketkotlin.domain.enums.*
import org.example.camticketkotlin.dto.UserDto
import org.example.camticketkotlin.repository.*
import org.example.camticketkotlin.scheduler.SeatCleanupScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * 고아 좌석 자동 해제 스케줄러 테스트
 */
@SpringBootTest
@TestPropertySource(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
class SeatCleanupSchedulerTest {

    @Autowired
    private lateinit var seatCleanupScheduler: SeatCleanupScheduler

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var scheduleSeatRepository: ScheduleSeatRepository

    @Autowired
    private lateinit var reservationSeatRepository: ReservationSeatRepository

    @Autowired
    private lateinit var reservationRequestRepository: ReservationRequestRepository

    @Autowired
    private lateinit var performanceScheduleRepository: PerformanceScheduleRepository

    @Autowired
    private lateinit var performancePostRepository: PerformancePostRepository

    @Autowired
    private lateinit var ticketOptionRepository: TicketOptionRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var artistUser: User
    private lateinit var customerUser: User
    private lateinit var testPost: PerformancePost
    private lateinit var testSchedule: PerformanceSchedule
    private lateinit var testTicketOption: TicketOption

    @BeforeEach
    fun setUp() {
        // 테스트 데이터 정리
        reservationSeatRepository.deleteAll()
        reservationRequestRepository.deleteAll()
        scheduleSeatRepository.deleteAll()
        ticketOptionRepository.deleteAll()
        performanceScheduleRepository.deleteAll()
        performancePostRepository.deleteAll()
        userRepository.deleteAll()

        // 테스트 데이터 생성
        artistUser = createUser("아티스트", "artist@test.com", Role.ROLE_MANAGER)
        customerUser = createUser("고객", "customer@test.com", Role.ROLE_USER)
        testPost = createPerformancePost()
        testSchedule = createPerformanceSchedule()
        testTicketOption = createTicketOption("일반", 10000)
    }

    @Test
    @DisplayName("고아 좌석만 해제되고, 예매와 연결된 좌석은 유지됨")
    @Transactional
    fun `should release only orphaned seats`() {
        // Given: 3개의 좌석 생성
        // 1. 고아 좌석 (11분 전 생성, 예매 없음)
        val orphanedSeat = ScheduleSeat(
            seatCode = "A1",
            status = SeatStatus.PENDING,
            performanceSchedule = testSchedule
        )
        val savedOrphanedSeat = scheduleSeatRepository.save(orphanedSeat)
        entityManager.flush()

        // regDate를 11분 전으로 직접 SQL 업데이트
        entityManager.createNativeQuery(
            "UPDATE schedule_seat SET reg_date = :oldDate WHERE id = :seatId"
        ).setParameter("oldDate", LocalDateTime.now().minusMinutes(11))
         .setParameter("seatId", savedOrphanedSeat.id)
         .executeUpdate()
        entityManager.flush()
        entityManager.clear()

        // 2. 예매와 연결된 좌석 (11분 전 생성, 예매 있음)
        val reservedSeat = ScheduleSeat(
            seatCode = "A2",
            status = SeatStatus.PENDING,
            performanceSchedule = testSchedule
        )
        val savedReservedSeat = scheduleSeatRepository.save(reservedSeat)
        entityManager.flush()

        entityManager.createNativeQuery(
            "UPDATE schedule_seat SET reg_date = :oldDate WHERE id = :seatId"
        ).setParameter("oldDate", LocalDateTime.now().minusMinutes(11))
         .setParameter("seatId", savedReservedSeat.id)
         .executeUpdate()
        entityManager.flush()
        entityManager.clear()

        // 예매 생성
        val reservation = ReservationRequest(
            performanceSchedule = testSchedule,
            ticketOption = testTicketOption,
            user = customerUser,
            count = 1,
            status = ReservationStatus.PENDING
        )
        val savedReservation = reservationRequestRepository.save(reservation)

        // 좌석-예매 연결
        val reservationSeat = ReservationSeat(
            reservationRequest = savedReservation,
            scheduleSeat = savedReservedSeat
        )
        reservationSeatRepository.save(reservationSeat)

        // 3. 최근 생성된 고아 좌석 (5분 전 생성, 예매 없음)
        val recentOrphanedSeat = ScheduleSeat(
            seatCode = "A3",
            status = SeatStatus.PENDING,
            performanceSchedule = testSchedule
        )
        val savedRecentSeat = scheduleSeatRepository.save(recentOrphanedSeat)
        entityManager.flush()

        entityManager.createNativeQuery(
            "UPDATE schedule_seat SET reg_date = :oldDate WHERE id = :seatId"
        ).setParameter("oldDate", LocalDateTime.now().minusMinutes(5))
         .setParameter("seatId", savedRecentSeat.id)
         .executeUpdate()
        entityManager.flush()
        entityManager.clear()

        println("\n========== 스케줄러 실행 전 ==========")
        println("총 좌석 수: ${scheduleSeatRepository.count()}")
        println("- 고아 좌석 (11분 전): A1")
        println("- 예매 연결 좌석 (11분 전): A2")
        println("- 최근 고아 좌석 (5분 전): A3")

        // When: 스케줄러 실행
        seatCleanupScheduler.releaseOrphanedSeats()

        // Then: 검증
        println("\n========== 스케줄러 실행 후 ==========")
        val remainingSeats = scheduleSeatRepository.findAll()
        println("남은 좌석 수: ${remainingSeats.size}")
        remainingSeats.forEach { seat ->
            println("- ${seat.seatCode} (생성시간: ${seat.regDate})")
        }

        assertEquals(2, remainingSeats.size, "2개의 좌석만 남아야 함")

        val remainingSeatCodes = remainingSeats.map { it.seatCode }.toSet()
        assert(!remainingSeatCodes.contains("A1")) { "고아 좌석 A1은 해제되어야 함" }
        assert(remainingSeatCodes.contains("A2")) { "예매 연결 좌석 A2는 유지되어야 함" }
        assert(remainingSeatCodes.contains("A3")) { "최근 고아 좌석 A3는 유지되어야 함 (10분 미경과)" }

        println("\n✅ 테스트 통과: 오래된 고아 좌석만 해제됨!")
    }

    @Test
    @DisplayName("고아 좌석이 없으면 아무것도 삭제하지 않음")
    fun `should not delete anything when no orphaned seats exist`() {
        // Given: 예매와 연결된 좌석만 존재
        val seat = ScheduleSeat(
            seatCode = "B1",
            status = SeatStatus.PENDING,
            performanceSchedule = testSchedule
        )
        val savedSeat = scheduleSeatRepository.save(seat)

        val reservation = ReservationRequest(
            performanceSchedule = testSchedule,
            ticketOption = testTicketOption,
            user = customerUser,
            count = 1,
            status = ReservationStatus.PENDING
        )
        val savedReservation = reservationRequestRepository.save(reservation)

        val reservationSeat = ReservationSeat(
            reservationRequest = savedReservation,
            scheduleSeat = savedSeat
        )
        reservationSeatRepository.save(reservationSeat)

        val beforeCount = scheduleSeatRepository.count()

        // When: 스케줄러 실행
        seatCleanupScheduler.releaseOrphanedSeats()

        // Then: 좌석 수 변화 없음
        val afterCount = scheduleSeatRepository.count()
        assertEquals(beforeCount, afterCount, "좌석 수가 변하지 않아야 함")

        println("✅ 테스트 통과: 예매 연결 좌석은 유지됨!")
    }

    // === 헬퍼 메서드들 ===

    private fun createUser(name: String, email: String, role: Role): User {
        val user = User.from(UserDto(
            kakaoId = System.currentTimeMillis() + (Math.random() * 1000000).toLong(),
            name = name,
            email = email,
            profileImageUrl = "test.jpg",
            role = role
        ))
        user.nickName = "${name}닉네임"
        return userRepository.save(user)
    }

    private fun createPerformancePost(): PerformancePost {
        val post = PerformancePost(
            title = "스케줄러 테스트 공연",
            category = PerformanceCategory.CONCERT,
            location = PerformanceLocation.HAKGWAN_104,
            ticketType = TicketType.PAID,
            maxTicketsPerUser = 4,
            backAccount = "1234-5678-9012",
            reservationStartAt = LocalDateTime.now().minusHours(1),
            reservationEndAt = LocalDateTime.now().plusDays(7),
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
}
