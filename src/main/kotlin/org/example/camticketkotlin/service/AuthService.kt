package org.example.camticketkotlin.service

import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.dto.UserDto
import org.example.camticketkotlin.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthService(
        private val userRepository: UserRepository,
        private val randomNicknameService: RandomNicknameService
) {

    // 카카오 로그인 로직
    fun kakaoLogin(dto: UserDto): UserDto {
        // ?: null이 아니면
        val kakaoId = dto.kakaoId ?: throw IllegalArgumentException("카카오 ID는 null일 수 없습니다.")

        val user = userRepository.findByKakaoId(kakaoId)
                .orElseGet {
                    val newUser = User.from(dto)
                    newUser.nickName = randomNicknameService.generateUniqueNickname()
                    userRepository.save(newUser)
                }

        user.email = dto.email
        user.profileImageUrl = dto.profileImageUrl
        user.name = dto.name

        return UserDto.toDto(user)
    }


    // 사용자 ID로 로그인한 사용자 정보 조회
    fun getLoginUser(userId: Long): User {
        return userRepository.findById(userId)
                .orElseThrow { IllegalArgumentException("해당 유저가 없습니다.") }
    }
}
