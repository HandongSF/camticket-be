package org.example.camticketkotlin.service

import org.example.camticketkotlin.domain.*
import org.example.camticketkotlin.dto.PerformancePostCreateDto
import org.example.camticketkotlin.dto.request.PerformancePostCreateRequest
import org.example.camticketkotlin.dto.response.PerformancePostDetailResponse
import org.example.camticketkotlin.exception.NotFoundException
import org.example.camticketkotlin.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile


@Service
class PerformancePostService(
    private val performancePostRepository: PerformancePostRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val ticketOptionRepository: TicketOptionRepository,
    private val performanceImageRepository: PerformanceImageRepository,
    private val s3Uploader: S3Uploader
) {

    @Transactional
    fun createPerformancePost(request: PerformancePostCreateRequest,
                              profileImage: MultipartFile,
                              detailImages: List<MultipartFile>,
                              user: User): PerformancePost {

        val profileImageUrl = s3Uploader.upload(profileImage, "camticket/profile")
        val detailImageUrls = s3Uploader.upload(detailImages, "camticket/detail")

        val dto = PerformancePostCreateDto.toDto(request, user, profileImageUrl, detailImageUrls)
        val post = performancePostRepository.save(PerformancePost.toEntity(dto)) // post 테이블 저장
        val schedules = dto.schedules // 공연 회차 저장
            .map { it.toEntity(post) }
            .map { performanceScheduleRepository.save(it) }

        dto.seatUnavailableCodesPerSchedule.forEach { seatData -> // 지정석 저장
            val targetSchedule = schedules.getOrNull(seatData.scheduleIndex)
                ?: throw IllegalArgumentException("유효하지 않은 공연 회차입니다 : ${seatData.scheduleIndex}")

            seatData.codes.forEach { seatCode ->
                scheduleSeatRepository.save(
                    ScheduleSeat(
                        seatCode = seatCode,
                        status = SeatStatus.UNAVAILABLE,
                        performanceSchedule = targetSchedule
                    )
                )
            }
        }


        dto.ticketOptions.forEach { ticketOptionRepository.save(it.toEntity(post)) } // 티켓 옵션 저장
        dto.detailImages.forEach { performanceImageRepository.save(it.toEntity(post)) } // 상세 이미지들 저장

        return post
    }

    fun getPostById(postId: Long): PerformancePostDetailResponse {
        val post = performancePostRepository.findById(postId)
            .orElseThrow { NotFoundException("해당 공연 게시글이 존재하지 않습니다.") }

        val schedules = performanceScheduleRepository.findByPerformancePost(post)
        val scheduleIndexMap = schedules.withIndex().associate { it.value.id!! to it.index }

        val scheduleDtos = schedules.mapIndexed { index, schedule ->
            PerformancePostDetailResponse.ScheduleDto(
                scheduleIndex = index,
                startTime = schedule.startTime.toString()
            )
        }

        val seatsBySchedule = scheduleSeatRepository.findByPerformanceScheduleIn(schedules)
            .groupBy { it.performanceSchedule.id!! }
            .map { (scheduleId, seats) ->
                PerformancePostDetailResponse.SeatUnavailableDto(
                    scheduleIndex = scheduleIndexMap[scheduleId] ?: -1,
                    codes = seats.map { it.seatCode }
                )
            }

        val ticketOptions = ticketOptionRepository.findByPerformancePost(post)
            .map {
                PerformancePostDetailResponse.TicketOptionDto(
                    name = it.name,
                    price = it.price
                )
            }

        val detailImages = performanceImageRepository.findByPerformancePost(post)
            .map { it.imageUrl }

        return PerformancePostDetailResponse(
            id = post.id!!,
            title = post.title,
            category = post.category.name,
            location = post.location.name,
            ticketType = post.ticketType.name,
            maxTicketsPerUser = post.maxTicketsPerUser,
            backAccount = post.backAccount,
            reservationStartAt = post.reservationStartAt.toString(),
            reservationEndAt = post.reservationEndAt.toString(),
            timeNotice = post.timeNotice,
            priceNotice = post.priceNotice,
            reservationNotice = post.reservationNotice,
            profileImageUrl = post.profileImageUrl,
            detailImageUrls = detailImages,
            schedules = scheduleDtos,
            seatUnavailableCodesPerSchedule = seatsBySchedule,
            ticketOptions = ticketOptions
        )
    }



}
