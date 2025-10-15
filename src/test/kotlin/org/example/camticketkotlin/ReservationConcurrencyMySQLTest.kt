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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@Testcontainers
class ReservationConcurrencyMySQLTest {

    companion object {
        @Container
        @JvmStatic
        val mysqlContainer = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
            withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl)
            registry.add("spring.datasource.username", mysqlContainer::getUsername)
            registry.add("spring.datasource.password", mysqlContainer::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.jpa.show-sql") { "true" }
            registry.add("spring.jpa.properties.hibernate.format_sql") { "true" }
            registry.add("logging.level.org.hibernate.SQL") { "DEBUG" }
        }
    }

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

    private lateinit var artistUser: User
    private lateinit var testPost: PerformancePost
    private lateinit var testSchedule: PerformanceSchedule
    private lateinit var testTicketOption: TicketOption

    @BeforeEach
    fun setUp() {
        // 공연 등록자
        val uniqueId = (System.currentTimeMillis() % 100000).toInt()
        artistUser = createUser("아티스트_$uniqueId", "art$uniqueId@test.com", Role.ROLE_MANAGER)

        // 공연 게시글 생성
        testPost = createPerformancePost()

        // 공연 회차 생성
        testSchedule = createPerformanceSchedule()

        // 티켓 옵션 생성
        testTicketOption = createTicketOption("일반", 10000)
    }

    @Test
    @DisplayName("MySQL 동시성 테스트 - 같은 좌석에 5명이 동시 예매 시도 (1명만 성공해야 함)")
    fun `concurrent reservation for same seat on MySQL should allow only one success`() {
        println("\n========== MySQL 동시성 테스트 시작 ==========")
        println("MySQL Container: ${mysqlContainer.jdbcUrl}")

        // Given
        val threadCount = 5  // Connection pool 이슈로 10 → 5로 변경
        val targetSeatCode = "A1"

        // 5명의 서로 다른 사용자 생성
        val uniqueId = (System.currentTimeMillis() % 100000).toInt() // 5자리 숫자로 제한
        val users = (1..threadCount).map { idx ->
            createUser("예매자${uniqueId}_$idx", "u${uniqueId}_$idx@test.com", Role.ROLE_USER)
        }

        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 10개 스레드가 동시에 같은 좌석 예매 시도
        users.forEachIndexed { index, user ->
            executor.submit {
                try {
                    val request = ReservationCreateRequest(
                        performancePostId = testPost.id!!,
                        performanceScheduleId = testSchedule.id!!,
                        selectedSeatCodes = listOf(targetSeatCode),
                        isPaymentCompleted = true,
                        userBankAccount = "1111-2222-3333",
                        ticketOrders = listOf(
                            TicketOrderItem(
                                ticketOptionId = testTicketOption.id!!,
                                count = 1,
                                unitPrice = 10000
                            )
                        )
                    )

                    reservationService.createReservation(user, request)
                    val currentSuccess = successCount.incrementAndGet()
                    println("✅ 스레드 $index (${user.name}) 예매 성공 (총 성공: $currentSuccess)")
                } catch (e: Exception) {
                    val currentFail = failCount.incrementAndGet()
                    println("❌ 스레드 $index (${user.name}) 예매 실패: ${e.message} (총 실패: $currentFail)")
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // Then - 결과 검증
        println("\n========== MySQL 동시성 테스트 결과 ==========")
        println("성공: ${successCount.get()}명")
        println("실패: ${failCount.get()}명")
        println("비관적 락 + 유니크 제약조건이 제대로 작동했는지 확인!")
        println("==========================================\n")

        // 1명만 성공해야 함
        assertEquals(1, successCount.get(), "같은 좌석은 1명만 예매 성공해야 함")
        assertEquals(4, failCount.get(), "나머지 4명은 실패해야 함")
    }

    @Test
    @DisplayName("MySQL 동시성 테스트 - 여러 사용자가 서로 다른 좌석 예매 (모두 성공해야 함)")
    fun `concurrent reservation for different seats on MySQL should all succeed`() {
        println("\n========== MySQL 서로 다른 좌석 테스트 시작 ==========")

        // Given
        val threadCount = 5
        val uniqueId = (System.currentTimeMillis() % 100000).toInt()
        val users = (1..threadCount).map { idx ->
            createUser("예매자${uniqueId}_$idx", "cust${uniqueId}_$idx@test.com", Role.ROLE_USER)
        }

        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 각각 다른 좌석 예매
        users.forEachIndexed { index, user ->
            executor.submit {
                try {
                    val seatCode = "A${index + 1}" // A1, A2, A3, A4, A5
                    val request = ReservationCreateRequest(
                        performancePostId = testPost.id!!,
                        performanceScheduleId = testSchedule.id!!,
                        selectedSeatCodes = listOf(seatCode),
                        isPaymentCompleted = true,
                        userBankAccount = "1111-2222-3333",
                        ticketOrders = listOf(
                            TicketOrderItem(
                                ticketOptionId = testTicketOption.id!!,
                                count = 1,
                                unitPrice = 10000
                            )
                        )
                    )

                    reservationService.createReservation(user, request)
                    val currentSuccess = successCount.incrementAndGet()
                    println("✅ ${user.name} - 좌석 $seatCode 예매 성공 (총 성공: $currentSuccess)")
                } catch (e: Exception) {
                    val currentFail = failCount.incrementAndGet()
                    println("❌ ${user.name} 예매 실패: ${e.message} (총 실패: $currentFail)")
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // Then - 서로 다른 좌석이므로 모두 성공해야 함
        println("\n========== MySQL 다른 좌석 테스트 결과 ==========")
        println("성공: ${successCount.get()}명")
        println("실패: ${failCount.get()}명")
        println("============================================\n")

        assertEquals(5, successCount.get(), "서로 다른 좌석이므로 5명 모두 성공해야 함")
        assertEquals(0, failCount.get(), "실패는 없어야 함")
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
            title = "MySQL 동시성 테스트 공연",
            category = PerformanceCategory.CONCERT,
            location = PerformanceLocation.HAKGWAN_104,
            ticketType = TicketType.PAID, // 지정석
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