package org.example.camticketkotlin.service

import org.example.camticketkotlin.domain.*
import org.example.camticketkotlin.domain.enums.*
import org.example.camticketkotlin.dto.UserDto
import org.example.camticketkotlin.dto.request.*
import org.example.camticketkotlin.exception.NotFoundException
import org.example.camticketkotlin.exception.UnauthorizedAccessException
import org.example.camticketkotlin.repository.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
])
@Transactional
class PerformanceManagementServiceIntegrationTest {

    @Autowired
    private lateinit var performanceManagementService: PerformanceManagementService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var performancePostRepository: PerformancePostRepository

    @Autowired
    private lateinit var performanceScheduleRepository: PerformanceScheduleRepository

    @Autowired
    private lateinit var ticketOptionRepository: TicketOptionRepository

    @Autowired
    private lateinit var performanceImageRepository: PerformanceImageRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        testUser = User.from(UserDto(
            kakaoId = 123456L,
            name = "테스트유저",
            email = "test@test.com",
            profileImageUrl = "test.jpg",
            role = Role.ROLE_MANAGER
        ))
        testUser.nickName = "테스트닉네임"
        testUser = userRepository.save(testUser)
    }

    @Test
    @DisplayName("공연 게시글 생성 성공 - 실제 파일 업로드 시뮬레이션")
    fun `createPerformancePost should create post successfully`() {
        // Given
        val request = PerformancePostCreateRequest(
            title = "새 공연",
            category = PerformanceCategory.CONCERT,
            location = PerformanceLocation.HAKGWAN_104,
            ticketType = TicketType.PAID,
            maxTicketsPerUser = 4,
            backAccount = "1234-5678-9012",
            reservationStartAt = LocalDateTime.now(),
            reservationEndAt = LocalDateTime.now().plusDays(7),
            timeNotice = "시간 안내",
            priceNotice = "가격 안내",
            reservationNotice = "예매 안내",
            schedules = listOf(
                ScheduleRequest(0, LocalDateTime.now().plusDays(1))
            ),
            seatUnavailableCodesPerSchedule = listOf(
                SeatUnavailableScheduleRequest(0, listOf("A1", "A2"))
            ),
            ticketOptions = listOf(
                TicketOptionRequest("일반", 10000)
            )
        )

        // Mock 파일 생성
        val profileImage = MockMultipartFile(
            "profileImage",
            "profile.jpg",
            "image/jpeg",
            "fake profile image content".toByteArray()
        )

        val detailImage = MockMultipartFile(
            "detailImage",
            "detail.jpg",
            "image/jpeg",
            "fake detail image content".toByteArray()
        )

        // When
        val result = performanceManagementService.createPerformancePost(
            request, profileImage, listOf(detailImage), testUser
        )

        // Then
        assertNotNull(result)
        assertNotNull(result.id)
        assertEquals("새 공연", result.title)
        assertEquals(PerformanceCategory.CONCERT, result.category)

        // 데이터베이스 검증
        val savedPost = performancePostRepository.findById(result.id!!).get()
        assertEquals("새 공연", savedPost.title)
        assertEquals(testUser.id, savedPost.user.id)

        // 스케줄 검증
        val schedules = performanceScheduleRepository.findByPerformancePost(savedPost)
        assertEquals(1, schedules.size)

        // 티켓 옵션 검증
        val ticketOptions = ticketOptionRepository.findByPerformancePost(savedPost)
        assertEquals(1, ticketOptions.size)
        assertEquals("일반", ticketOptions[0].name)
        assertEquals(10000, ticketOptions[0].price)
    }

    @Test
    @DisplayName("공연 게시글 조회 성공")
    fun `getPostById should return post successfully`() {
        // Given - 실제 게시글 생성
        val post = createTestPerformancePost()

        // When
        val result = performanceManagementService.getPostById(post.id!!)

        // Then
        assertNotNull(result)
        assertEquals(post.id, result.id)
        assertEquals("테스트 공연", result.title)
        assertEquals("CONCERT", result.category)
        assertEquals("HAKGWAN_104", result.location)
        assertTrue(result.schedules.isNotEmpty())
        assertTrue(result.ticketOptions.isNotEmpty())
    }

    @Test
    @DisplayName("공연 게시글 조회 - 존재하지 않는 게시글")
    fun `getPostById should throw exception when post not found`() {
        // Given
        val nonExistentPostId = 999L

        // When & Then
        val exception = assertThrows<NotFoundException> {
            performanceManagementService.getPostById(nonExistentPostId)
        }

        assertEquals("해당 공연 게시글이 존재하지 않습니다.", exception.message)
    }

    @Test
    @DisplayName("사용자의 공연 오버뷰 조회")
    fun `getOverviewByUser should return user performances`() {
        // Given - 사용자의 공연 생성
        val post1 = createTestPerformancePost("공연1")
        val post2 = createTestPerformancePost("공연2")

        // When
        val result = performanceManagementService.getOverviewByUser(testUser)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.postId == post1.id })
        assertTrue(result.any { it.postId == post2.id })
    }

    @Test
    @DisplayName("공연 게시글 삭제 성공")
    fun `deletePerformancePost should delete post successfully`() {
        // Given
        val post = createTestPerformancePost()

        // When
        performanceManagementService.deletePerformancePost(post.id!!, testUser)

        // Then
        val deletedPost = performancePostRepository.findById(post.id!!)
        assertTrue(deletedPost.isEmpty, "게시글이 삭제되어야 함")

        // 연관 데이터도 삭제되었는지 확인
        val schedules = performanceScheduleRepository.findByPerformancePost(post)
        assertTrue(schedules.isEmpty(), "스케줄도 삭제되어야 함")

        val ticketOptions = ticketOptionRepository.findByPerformancePost(post)
        assertTrue(ticketOptions.isEmpty(), "티켓 옵션도 삭제되어야 함")
    }

    @Test
    @DisplayName("공연 게시글 삭제 - 권한 없음 예외")
    fun `deletePerformancePost should throw exception when user has no permission`() {
        // Given
        val post = createTestPerformancePost()

        // 다른 사용자 생성
        val otherUser = User.from(UserDto(
            kakaoId = 789012L,
            name = "다른유저",
            email = "other@test.com",
            profileImageUrl = "other.jpg",
            role = Role.ROLE_USER
        ))
        otherUser.nickName = "다른닉네임"
        val savedOtherUser = userRepository.save(otherUser)

        // When & Then
        val exception = assertThrows<UnauthorizedAccessException> {
            performanceManagementService.deletePerformancePost(post.id!!, savedOtherUser)
        }

        assertEquals("접근 권한이 없습니다.", exception.message)
    }

    @Test
    @DisplayName("공연 게시글 삭제 - 존재하지 않는 게시글")
    fun `deletePerformancePost should throw exception when post not found`() {
        // Given
        val nonExistentPostId = 999L

        // When & Then
        val exception = assertThrows<NotFoundException> {
            performanceManagementService.deletePerformancePost(nonExistentPostId, testUser)
        }

        assertEquals("해당 공연 게시글이 존재하지 않습니다.", exception.message)
    }

    @Test
    @DisplayName("공연 게시글 수정 성공")
    fun `updatePerformancePost should update post successfully`() {
        // Given
        val originalPost = createTestPerformancePost()

        val updateRequest = PerformancePostUpdateRequest(
            id = originalPost.id!!,
            title = "수정된 공연",
            category = PerformanceCategory.PLAY,
            location = PerformanceLocation.BUSAN,
            ticketType = TicketType.FREE,
            maxTicketsPerUser = 6,
            backAccount = "9876-5432-1098",
            reservationStartAt = LocalDateTime.now().plusHours(1),
            reservationEndAt = LocalDateTime.now().plusDays(5),
            timeNotice = "수정된 시간 안내",
            priceNotice = "수정된 가격 안내",
            reservationNotice = "수정된 예매 안내",
            schedules = listOf(
                ScheduleRequest(0, LocalDateTime.now().plusDays(2))
            ),
            seatUnavailableCodesPerSchedule = listOf(
                SeatUnavailableScheduleRequest(0, listOf("B1", "B2"))
            ),
            ticketOptions = listOf(
                TicketOptionRequest("수정된 옵션", 15000)
            )
        )

        val newDetailImage = MockMultipartFile(
            "newDetailImage",
            "new_detail.jpg",
            "image/jpeg",
            "new detail image content".toByteArray()
        )

        // When
        val result = performanceManagementService.updatePerformancePost(
            originalPost.id!!, updateRequest, listOf(newDetailImage), testUser
        )

        // Then
        assertEquals(originalPost.id, result)

        // 데이터베이스에서 확인
        val updatedPost = performancePostRepository.findById(originalPost.id!!).get()
        assertEquals("수정된 공연", updatedPost.title)
        assertEquals(PerformanceCategory.PLAY, updatedPost.category)
        assertEquals(PerformanceLocation.BUSAN, updatedPost.location)
        assertEquals(6, updatedPost.maxTicketsPerUser)

        // 새로운 스케줄 확인
        val updatedSchedules = performanceScheduleRepository.findByPerformancePost(updatedPost)
        assertEquals(1, updatedSchedules.size)

        // 새로운 티켓 옵션 확인
        val updatedTicketOptions = ticketOptionRepository.findByPerformancePost(updatedPost)
        assertEquals(1, updatedTicketOptions.size)
        assertEquals("수정된 옵션", updatedTicketOptions[0].name)
        assertEquals(15000, updatedTicketOptions[0].price)
    }

    // 헬퍼 메서드: 테스트용 공연 게시글 생성
    private fun createTestPerformancePost(title: String = "테스트 공연"): PerformancePost {
        val post = PerformancePost(
            title = title,
            category = PerformanceCategory.CONCERT,
            location = PerformanceLocation.HAKGWAN_104,
            ticketType = TicketType.PAID,
            maxTicketsPerUser = 4,
            backAccount = "1234-5678-9012",
            reservationStartAt = LocalDateTime.now(),
            reservationEndAt = LocalDateTime.now().plusDays(7),
            timeNotice = "시간 안내",
            priceNotice = "가격 안내",
            reservationNotice = "예매 안내",
            profileImageUrl = "https://test.com/profile.jpg",
            user = testUser
        )

        val savedPost = performancePostRepository.save(post)

        // 스케줄 추가
        val schedule = PerformanceSchedule(
            startTime = LocalDateTime.now().plusDays(1),
            performancePost = savedPost
        )
        performanceScheduleRepository.save(schedule)

        // 티켓 옵션 추가
        val ticketOption = TicketOption(
            name = "일반",
            price = 10000,
            performancePost = savedPost
        )
        ticketOptionRepository.save(ticketOption)

        return savedPost
    }
}