package org.example.camticketkotlin.service

import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.domain.enums.Role
import org.example.camticketkotlin.dto.UserDto
import org.example.camticketkotlin.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
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
@Transactional  // 각 테스트 후 롤백
class AuthServiceIntegrationTest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 데이터 정리
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("신규 사용자 카카오 로그인 - 실제 데이터베이스 테스트")
    fun `kakaoLogin should create new user in database`() {
        // Given
        val kakaoId = 123456L
        val userDto = UserDto(
            kakaoId = kakaoId,
            name = "홍길동",
            email = "hong@kakao.com",
            profileImageUrl = "https://example.com/profile.jpg",
            role = Role.ROLE_USER
        )

        // 신규 사용자인지 확인
        val existingUser = userRepository.findByKakaoId(kakaoId)
        assertTrue(existingUser.isEmpty, "처음에는 사용자가 없어야 함")

        // When
        val result = authService.kakaoLogin(userDto)

        // Then
        assertNotNull(result)
        assertEquals(kakaoId, result.kakaoId)
        assertEquals("홍길동", result.name)
        assertEquals("hong@kakao.com", result.email)
        assertEquals("https://example.com/profile.jpg", result.profileImageUrl)
        assertEquals(Role.ROLE_USER, result.role)
        assertNotNull(result.nickName)  // 랜덤 닉네임이 생성되어야 함
        assertTrue(result.nickName!!.isNotEmpty(), "닉네임이 생성되어야 함")

        // 데이터베이스에 실제로 저장되었는지 확인
        val savedUser = userRepository.findByKakaoId(kakaoId)
        assertTrue(savedUser.isPresent, "데이터베이스에 사용자가 저장되어야 함")
        assertEquals("홍길동", savedUser.get().name)
        assertEquals("hong@kakao.com", savedUser.get().email)
    }

    @Test
    @DisplayName("기존 사용자 카카오 로그인 - 정보 업데이트 테스트")
    fun `kakaoLogin should update existing user info in database`() {
        // Given - 먼저 사용자를 생성
        val kakaoId = 123456L
        val initialUser = User.from(UserDto(
            kakaoId = kakaoId,
            name = "기존이름",
            email = "old@kakao.com",
            profileImageUrl = "old_profile.jpg",
            role = Role.ROLE_USER
        ))
        initialUser.nickName = "기존닉네임"
        userRepository.save(initialUser)

        // 업데이트할 정보
        val updatedUserDto = UserDto(
            kakaoId = kakaoId,
            name = "새이름",
            email = "new@kakao.com",
            profileImageUrl = "new_profile.jpg",
            role = Role.ROLE_USER
        )

        // When
        val result = authService.kakaoLogin(updatedUserDto)

        // Then
        assertEquals("새이름", result.name)
        assertEquals("new@kakao.com", result.email)
        assertEquals("new_profile.jpg", result.profileImageUrl)
        assertEquals("기존닉네임", result.nickName)  // 닉네임은 변경되지 않음

        // 데이터베이스에서 확인
        val updatedUser = userRepository.findByKakaoId(kakaoId)
        assertTrue(updatedUser.isPresent)
        assertEquals("새이름", updatedUser.get().name)
        assertEquals("new@kakao.com", updatedUser.get().email)
        assertEquals("new_profile.jpg", updatedUser.get().profileImageUrl)
        assertEquals("기존닉네임", updatedUser.get().nickName)
    }

    @Test
    @DisplayName("null kakaoId로 로그인 시도 - 예외 발생")
    fun `kakaoLogin should throw exception when kakaoId is null`() {
        // Given
        val userDto = UserDto(
            kakaoId = null,
            name = "홍길동",
            email = "test@test.com",
            role = Role.ROLE_USER
        )

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.kakaoLogin(userDto)
        }

        assertEquals("카카오 ID는 null일 수 없습니다.", exception.message)

        // 데이터베이스에 저장되지 않았는지 확인
        val allUsers = userRepository.findAll()
        assertTrue(allUsers.isEmpty(), "예외 발생 시 사용자가 저장되지 않아야 함")
    }

    @Test
    @DisplayName("사용자 조회 성공 - 실제 데이터베이스")
    fun `getLoginUser should return user from database`() {
        // Given - 실제 사용자 생성
        val testUser = User.from(UserDto(
            kakaoId = 123456L,
            name = "테스트유저",
            email = "test@test.com",
            profileImageUrl = "test.jpg",
            role = Role.ROLE_USER
        ))
        val savedUser = userRepository.save(testUser)

        // When
        val result = authService.getLoginUser(savedUser.id!!)

        // Then
        assertNotNull(result)
        assertEquals(savedUser.id, result.id)
        assertEquals("테스트유저", result.name)
        assertEquals("test@test.com", result.email)
        assertEquals(Role.ROLE_USER, result.role)
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 - 예외 발생")
    fun `getLoginUser should throw exception when user not found in database`() {
        // Given
        val nonExistentUserId = 999L

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.getLoginUser(nonExistentUserId)
        }

        assertEquals("해당 유저가 없습니다.", exception.message)
    }

    @Test
    @DisplayName("닉네임 생성 확인 - 실제 RandomNicknameService 동작")
    fun `kakaoLogin should generate unique nickname for new user`() {
        // Given
        val userDto1 = UserDto(
            kakaoId = 111111L,
            name = "사용자1",
            email = "user1@test.com",
            profileImageUrl = "user1.jpg",
            role = Role.ROLE_USER
        )

        val userDto2 = UserDto(
            kakaoId = 222222L,
            name = "사용자2",
            email = "user2@test.com",
            profileImageUrl = "user2.jpg",
            role = Role.ROLE_USER
        )

        // When
        val result1 = authService.kakaoLogin(userDto1)
        val result2 = authService.kakaoLogin(userDto2)

        // Then
        assertNotNull(result1.nickName)
        assertNotNull(result2.nickName)
        assertTrue(result1.nickName!!.isNotEmpty())
        assertTrue(result2.nickName!!.isNotEmpty())

        // 닉네임이 서로 다른지 확인 (유니크 생성)
        assertTrue(result1.nickName != result2.nickName, "각 사용자는 고유한 닉네임을 가져야 함")

        // 데이터베이스에도 제대로 저장되었는지 확인
        val user1 = userRepository.findByKakaoId(111111L).get()
        val user2 = userRepository.findByKakaoId(222222L).get()
        assertEquals(result1.nickName, user1.nickName)
        assertEquals(result2.nickName, user2.nickName)
    }
}