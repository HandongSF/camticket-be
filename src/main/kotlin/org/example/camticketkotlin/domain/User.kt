package org.example.camticket.domain

import jakarta.persistence.*
import org.example.camticket.dto.UserDto

@Entity
class User private constructor(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long? = null,

        val kakaoId: Long,

        @Column(columnDefinition = "varchar(200)")
        var name: String? = null,

        @Column(columnDefinition = "varchar(30)")
        var nickName: String? = null,

        @Column(columnDefinition = "varchar(30)")
        var email: String? = null,

        @Column(columnDefinition = "TEXT")
        var profileImageUrl: String? = null,

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        val role: Role
) : BaseEntity() {

    companion object {
        fun from(dto: UserDto): User {
            return User(
                    kakaoId = dto.kakaoId,
                    nickName = dto.nickName,
                    email = dto.email,
                    profileImageUrl = dto.profileImageUrl,
                    role = Role.ROLE_USER // 기본 권한
            )
        }
    }
}
