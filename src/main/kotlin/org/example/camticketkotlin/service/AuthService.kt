package org.example.camticket.service

import org.example.camticket.domain.User
import org.example.camticket.dto.UserDto
import org.example.camticket.repository.UserRepository
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
        val user = userRepository.findByKakaoId(dto.kakaoId)
                .orElseGet {
            val newUser = User.from(dto)
            newUser.nickName = randomNicknameService.generateUniqueNickname()
            userRepository.save(newUser)
        }

        user.email = dto.email
        user.profileImageUrl = dto.profileImageUrl
        user.name = dto.name

        return UserDto.from(user)
    }

    // 사용자 ID로 로그인한 사용자 정보 조회
    fun getLoginUser(userId: Long): User {
        return userRepository.findById(userId)
                .orElseThrow { IllegalArgumentException("해당 유저가 없습니다.") }
    }
}
