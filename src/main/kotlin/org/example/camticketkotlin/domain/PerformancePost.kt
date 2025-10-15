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

    @Column(name = "is_closed", nullable = false)
    var isClosed: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @OneToMany(
    mappedBy = "performancePost",
    fetch = FetchType.LAZY,
    cascade = [CascadeType.ALL],
    orphanRemoval = true
    )
    val schedules: List<PerformanceSchedule> = mutableListOf()

) : BaseEntity() {

    // ========== DDD: 도메인 로직 ========== //

    /**
     * 예매 가능 여부 확인
     */
    fun isReservationOpen(): Boolean {
        val now = LocalDateTime.now()
        return !isClosed && now.isAfter(reservationStartAt) && now.isBefore(reservationEndAt)
    }

    /**
     * 예매 가능 상태 확인 (예외 발생)
     */
    fun validateReservationAvailable() {
        require(!isClosed) { "마감된 공연입니다." }
        val now = LocalDateTime.now()
        require(now.isAfter(reservationStartAt)) { "예매 시작 전입니다. 예매 시작: $reservationStartAt" }
        require(now.isBefore(reservationEndAt)) { "예매 기간이 종료되었습니다." }
    }

    /**
     * 공연 마감
     */
    fun close() {
        require(!isClosed) { "이미 마감된 공연입니다." }
        this.isClosed = true
    }

    /**
     * 공연 재오픈
     */
    fun reopen() {
        require(isClosed) { "마감되지 않은 공연입니다." }
        this.isClosed = false
    }

    /**
     * 공연 정보 업데이트
     */
    fun updateDetails(
        newTitle: String,
        newCategory: PerformanceCategory,
        newLocation: PerformanceLocation,
        newTicketType: TicketType,
        newMaxTicketsPerUser: Int,
        newBackAccount: String,
        newReservationStartAt: LocalDateTime,
        newReservationEndAt: LocalDateTime,
        newTimeNotice: String,
        newPriceNotice: String,
        newReservationNotice: String
    ) {
        require(newTitle.isNotBlank()) { "제목은 비어있을 수 없습니다." }
        require(newMaxTicketsPerUser > 0) { "최대 예매 가능 매수는 1 이상이어야 합니다." }
        require(newReservationStartAt.isBefore(newReservationEndAt)) { "예매 시작일은 종료일보다 이전이어야 합니다." }

        this.title = newTitle
        this.category = newCategory
        this.location = newLocation
        this.ticketType = newTicketType
        this.maxTicketsPerUser = newMaxTicketsPerUser
        this.backAccount = newBackAccount
        this.reservationStartAt = newReservationStartAt
        this.reservationEndAt = newReservationEndAt
        this.timeNotice = newTimeNotice
        this.priceNotice = newPriceNotice
        this.reservationNotice = newReservationNotice
    }

    /**
     * 사용자 최대 예매 가능 매수 검증
     */
    fun validateTicketCount(requestedCount: Int) {
        require(requestedCount > 0) { "예매 매수는 1 이상이어야 합니다." }
        require(requestedCount <= maxTicketsPerUser) {
            "최대 예매 가능 매수는 ${maxTicketsPerUser}매입니다. 요청: ${requestedCount}매"
        }
    }

    /**
     * 공연 등록자 확인
     */
    fun isOwnedBy(userId: Long): Boolean {
        return this.user.id == userId
    }

    @Deprecated("Use updateDetails() instead for better domain encapsulation")
    fun updateFromDto(dto: PerformancePostUpdateRequest) {
        updateDetails(
            newTitle = dto.title,
            newCategory = dto.category,
            newLocation = dto.location,
            newTicketType = dto.ticketType,
            newMaxTicketsPerUser = dto.maxTicketsPerUser,
            newBackAccount = dto.backAccount,
            newReservationStartAt = dto.reservationStartAt,
            newReservationEndAt = dto.reservationEndAt,
            newTimeNotice = dto.timeNotice,
            newPriceNotice = dto.priceNotice,
            newReservationNotice = dto.reservationNotice
        )
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

