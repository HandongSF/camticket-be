package org.example.camticketkotlin.service

import org.example.camticketkotlin.dto.response.PerformancePostOverviewResponse
import org.example.camticketkotlin.repository.PerformancePostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PerformancePostService(
    private val performancePostRepository: PerformancePostRepository
) {

    @Transactional(readOnly = true)
    fun getAllPerformancesOverview(): List<PerformancePostOverviewResponse> {
        val posts = performancePostRepository.findAll() // 혹은 JOIN FETCH 버전

        return posts.map { post ->
            val firstSchedule = post.schedules.minByOrNull { it.startTime }
                ?: throw IllegalStateException("공연 ID=${post.id}에 회차 정보가 없습니다.")

            PerformancePostOverviewResponse(
                postId = post.id!!,
                title = post.title,
                profileImageUrl = post.profileImageUrl,
                reservationStartAt = post.reservationStartAt,
                reservationEndAt = post.reservationEndAt,
                firstScheduleStartTime = firstSchedule.startTime,
                location = post.location.name,
                userId = post.user.id!!,
                category = post.category
            )
        }
    }

}
