package org.example.camticketkotlin.domain

import jakarta.persistence.*
import org.example.camticketkotlin.domain.enums.PerformanceCategory
import org.example.camticketkotlin.domain.enums.PerformanceLocation
import org.example.camticketkotlin.domain.enums.TicketType
import org.example.camticketkotlin.dto.PerformancePostCreateDto
import java.time.LocalDateTime

@Entity
class PerformancePost(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var title: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: PerformanceCategory,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var location: PerformanceLocation,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var ticketType: TicketType,

    @Column(nullable = false)
    var maxTicketsPerUser: Int,

    @Column(nullable = false)
    var backAccount: String,

    @Column(nullable = false)
    var reservationStartAt: LocalDateTime,

    @Column(nullable = false)
    var reservationEndAt: LocalDateTime,

    @Column(nullable = false, columnDefinition = "TEXT")
    var timeNotice: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var priceNotice: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var reservationNotice: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var profileImageUrl: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User
) : BaseEntity() {

    companion object {
        fun toEntity(dto: PerformancePostCreateDto): PerformancePost {
            return PerformancePost(
                title = dto.title,
                category = PerformanceCategory.valueOf(dto.category),
                location = PerformanceLocation.valueOf(dto.location),
                ticketType = TicketType.valueOf(dto.ticketType),
                maxTicketsPerUser = dto.maxTicketsPerUser,
                backAccount = dto.backAccount,
                reservationStartAt = dto.reservationStartAt,
                reservationEndAt = dto.reservationEndAt,
                timeNotice = dto.timeNotice,
                priceNotice = dto.priceNotice,
                reservationNotice = dto.reservationNotice,
                profileImageUrl = dto.profileImageUrl,
                user = dto.user
            )
        }
    }
}

