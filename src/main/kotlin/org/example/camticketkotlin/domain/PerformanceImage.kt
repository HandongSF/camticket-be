package org.example.camticketkotlin.domain

import jakarta.persistence.*

@Entity
class PerformanceImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var imageUrl: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_post_id", nullable = false)
    var performancePost: PerformancePost
) {

    // ========== DDD: 도메인 로직 ========== //

    /**
     * 이미지 URL 검증
     */
    init {
        require(imageUrl.isNotBlank()) { "이미지 URL은 비어있을 수 없습니다." }
    }

    /**
     * 이미지 URL 업데이트
     */
    fun updateImageUrl(newImageUrl: String) {
        require(newImageUrl.isNotBlank()) { "이미지 URL은 비어있을 수 없습니다." }
        this.imageUrl = newImageUrl
    }

    /**
     * 특정 공연에 속하는지 확인
     */
    fun belongsTo(performancePostId: Long): Boolean {
        return this.performancePost.id == performancePostId
    }

    companion object {
        /**
         * 팩토리 메서드: 새 공연 이미지 생성
         */
        fun create(imageUrl: String, performancePost: PerformancePost): PerformanceImage {
            require(imageUrl.isNotBlank()) { "이미지 URL은 비어있을 수 없습니다." }
            return PerformanceImage(
                imageUrl = imageUrl,
                performancePost = performancePost
            )
        }
    }
}
