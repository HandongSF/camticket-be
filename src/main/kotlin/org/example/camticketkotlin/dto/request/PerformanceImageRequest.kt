package org.example.camticketkotlin.dto.request

import org.example.camticketkotlin.domain.PerformanceImage
import org.example.camticketkotlin.domain.PerformancePost

data class PerformanceImageRequest(
    val imageUrl: String
) {
    fun toEntity(post: PerformancePost): PerformanceImage {
        return PerformanceImage(
            imageUrl = this.imageUrl,
            performancePost = post
        )
    }
}
