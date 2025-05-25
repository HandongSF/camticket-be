package org.example.camticketkotlin.domain

import jakarta.persistence.*
import org.example.camticketkotlin.domain.enums.PerformanceCategory
import org.example.camticketkotlin.domain.enums.PerformanceLocation
import org.example.camticketkotlin.domain.enums.TicketType
import org.example.camticketkotlin.dto.PerformancePostCreateDto
import org.example.camticketkotlin.dto.request.PerformancePostUpdateRequest
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

    fun updateFromDto(dto: PerformancePostUpdateRequest) {
        this.title = dto.title
        this.category = dto.category
        this.location = dto.location
        this.ticketType = dto.ticketType
        this.maxTicketsPerUser = dto.maxTicketsPerUser
        this.backAccount = dto.backAccount
        this.reservationStartAt = dto.reservationStartAt
        this.reservationEndAt = dto.reservationEndAt
        this.timeNotice = dto.timeNotice
        this.priceNotice = dto.priceNotice
        this.reservationNotice = dto.reservationNotice
    }

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

