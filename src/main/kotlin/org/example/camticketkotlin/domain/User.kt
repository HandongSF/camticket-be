package org.example.camticketkotlin.domain

import jakarta.persistence.*
import org.example.camticketkotlin.domain.enums.Role
import org.example.camticketkotlin.dto.UserDto

@Entity
@Table(name = "`user`")
class User private constructor(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long? = null,

        @Column(nullable = false)
        val kakaoId: Long? = null,

        @Column(nullable = false, length = 200)
        var name: String? = null,

        @Column(nullable = true, length = 30)
        var nickName: String? = null,

        @Column(nullable = false, length = 30)
        var email: String? = null,

        @Column(nullable = false, columnDefinition = "TEXT")
        var profileImageUrl: String? = null,

        @Column(nullable = true, length = 500)
        var introduction: String? = null,

        @Column(nullable = true, length = 100)
        var bankAccount: String? = null,

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        val role: Role


) : BaseEntity() {

    // ========== DDD: 도메인 로직 ========== //

    /**
     * 프로필 업데이트
     */
    fun updateProfile(
        newNickName: String?,
        newIntroduction: String?,
        newBankAccount: String?
    ) {
        newNickName?.let {
            require(it.isNotBlank()) { "닉네임은 비어있을 수 없습니다." }
            require(it.length <= 30) { "닉네임은 30자 이하여야 합니다." }
            this.nickName = it
        }
        newIntroduction?.let { this.introduction = it }
        newBankAccount?.let { this.bankAccount = it }
    }

    /**
     * 프로필 이미지 업데이트
     */
    fun updateProfileImage(newImageUrl: String) {
        require(newImageUrl.isNotBlank()) { "프로필 이미지 URL은 비어있을 수 없습니다." }
        this.profileImageUrl = newImageUrl
    }

    /**
     * 매니저 권한 확인
     */
    fun isManager(): Boolean = role == Role.ROLE_MANAGER

    /**
     * 일반 사용자 권한 확인
     */
    fun isUser(): Boolean = role == Role.ROLE_USER

    /**
     * 계좌 정보 등록 여부 확인
     */
    fun hasBankAccount(): Boolean = !bankAccount.isNullOrBlank()

    /**
     * 사용자 본인 확인
     */
    fun isOwnedBy(userId: Long): Boolean = this.id == userId

    companion object {
        /**
         * 팩토리 메서드: Kakao 정보로 사용자 생성
         */
        fun from(dto: UserDto): User {
            return User(
                kakaoId = requireNotNull(dto.kakaoId) { "Kakao ID는 필수입니다." },
                name = requireNotNull(dto.name) { "이름은 필수입니다." },
                nickName = dto.nickName ?: "",
                email = requireNotNull(dto.email) { "이메일은 필수입니다." },
                profileImageUrl = requireNotNull(dto.profileImageUrl) { "프로필 이미지는 필수입니다." },
                introduction = dto.introduction ?: "",
                bankAccount = dto.bankAccount ?: "",
                role = dto.role
            )
        }
    }
}
