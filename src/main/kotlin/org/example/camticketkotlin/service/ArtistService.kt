package org.example.camticketkotlin.service

import org.example.camticketkotlin.dto.response.ArtistPerformanceSummaryResponse
import org.example.camticketkotlin.repository.PerformancePostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArtistService(
    private val performancePostRepository: PerformancePostRepository
) {

    @Transactional(readOnly = true)
    fun getPerformanceSummariesByUserId(userId: Long): List<ArtistPerformanceSummaryResponse> {
        val posts = performancePostRepository.findAllByUserId(userId)

        return posts.map {
            ArtistPerformanceSummaryResponse(
                postId = it.id!!,
                profileImageUrl = it.profileImageUrl
            )
        }
    }
}
