package org.example.camticketkotlin.domain

import jakarta.persistence.*
import org.example.camticketkotlin.domain.enums.Role
import org.example.camticketkotlin.dto.UserDto

@Entity
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

        @Column(nullable = true, length = 100)
        var bankAccount: String,

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        val role: Role
) : BaseEntity() {

    companion object {
        fun from(dto: UserDto): User {
            return User(
                kakaoId = requireNotNull(dto.kakaoId),
                name = requireNotNull(dto.name),
                nickName = dto.nickName ?: "",
                email = requireNotNull(dto.email),
                profileImageUrl = requireNotNull(dto.profileImageUrl),
                bankAccount = dto.bankAccount ?: "",
                role = Role.ROLE_USER
            )
        }
    }
}
