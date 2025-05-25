package org.example.camticketkotlin.dto

import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.domain.enums.Role

// data class ➝ 데이터를 담는 그릇
data class UserDto(
        // 각 변수는 null일 수 있고, 생성자에서 전부 초기화할 수 있음.
        val id: Long? = null,
        val kakaoId: Long? = null,
        var name: String? = null,
        var nickName: String? = null,
        var email: String? = null,
        var profileImageUrl: String? = null,
        val introduction: String? = null,
        var bankAccount: String? = null,
        val role: Role // ← 추가
) {
    // 자바의 static 메서드랑 똑같은 기능
    companion object {
        // User라는 엔티티 객체를 받아서, 필요한 정보만 꺼내서 UserDto로 바꿔주는 변환기
        // companion object ➝ 변환기 (Entity → DTO 변환)
        fun toDto(user: User): UserDto {
            return UserDto(
                    id = user.id,
                    kakaoId = user.kakaoId,
                    name = user.name,
                    nickName = user.nickName,
                    email = user.email,
                    profileImageUrl = user.profileImageUrl,
                    introduction = user.introduction,
                    bankAccount = user.bankAccount,
                    role = user.role
            )
        }

        fun from(users: List<User>): List<UserDto> {
            return users.map { toDto(it) }
        }
    }
}
