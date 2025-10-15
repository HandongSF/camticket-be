package org.example.camticketkotlin.domain

import jakarta.persistence.*

@Entity
class TicketOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50)
    var name: String,  // e.g., 일반 or 새내기

    @Column(nullable = false)
    var price: Int,  // 단위: 원

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_post_id", nullable = false)
    var performancePost: PerformancePost
) {

    // ========== DDD: 도메인 로직 ========== //

    /**
     * 티켓 옵션 정보 업데이트
     */
    fun updateDetails(newName: String, newPrice: Int) {
        require(newName.isNotBlank()) { "티켓 옵션 이름은 비어있을 수 없습니다." }
        require(newName.length <= 50) { "티켓 옵션 이름은 50자 이하여야 합니다." }
        require(newPrice >= 0) { "티켓 가격은 0원 이상이어야 합니다." }

        this.name = newName
        this.price = newPrice
    }

    /**
     * 무료 티켓인지 확인
     */
    fun isFree(): Boolean = price == 0

    /**
     * 유료 티켓인지 확인
     */
    fun isPaid(): Boolean = price > 0

    /**
     * 특정 수량의 총 금액 계산
     */
    fun calculateTotalPrice(quantity: Int): Int {
        require(quantity > 0) { "수량은 1 이상이어야 합니다." }
        return price * quantity
    }

    /**
     * 가격 변경
     */
    fun updatePrice(newPrice: Int) {
        require(newPrice >= 0) { "티켓 가격은 0원 이상이어야 합니다." }
        this.price = newPrice
    }

    companion object {
        /**
         * 팩토리 메서드: 새 티켓 옵션 생성
         */
        fun create(name: String, price: Int, performancePost: PerformancePost): TicketOption {
            require(name.isNotBlank()) { "티켓 옵션 이름은 비어있을 수 없습니다." }
            require(name.length <= 50) { "티켓 옵션 이름은 50자 이하여야 합니다." }
            require(price >= 0) { "티켓 가격은 0원 이상이어야 합니다." }

            return TicketOption(
                name = name,
                price = price,
                performancePost = performancePost
            )
        }
    }
}
