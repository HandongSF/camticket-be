package org.example.camticketkotlin.service

import org.example.camticketkotlin.domain.*
import org.example.camticketkotlin.dto.PerformancePostCreateDto
import org.example.camticketkotlin.dto.request.PerformancePostCreateRequest
import org.example.camticketkotlin.dto.request.PerformancePostUpdateRequest
import org.example.camticketkotlin.dto.response.PerformanceManagementOverviewResponse
import org.example.camticketkotlin.dto.response.PerformancePostDetailResponse
import org.example.camticketkotlin.exception.NotFoundException
import org.example.camticketkotlin.exception.UnauthorizedAccessException
import org.example.camticketkotlin.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime


@Service
class PerformanceManagementService(
    private val performancePostRepository: PerformancePostRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val ticketOptionRepository: TicketOptionRepository,
    private val performanceImageRepository: PerformanceImageRepository,
    private val reservationRequestRepository: ReservationRequestRepository,
    private val reservationSeatRepository: ReservationSeatRepository,
    private val s3Uploader: S3Uploader,


) {

    companion object {
        private val logger = LoggerFactory.getLogger(PerformanceManagementService::class.java)
    }

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

    fun getOverviewByUser(user: User): List<PerformanceManagementOverviewResponse> {
        val posts = performancePostRepository.findAllByUserId(user.id!!)

        val latestTimes = performanceScheduleRepository
            .findLatestScheduleTimes(posts.map { it.id!! })
            .associate { (postId, latestTime) ->
                postId as Long to (latestTime as LocalDateTime).toString()
            }

        return posts.mapNotNull { post ->
            val lastTime = latestTimes[post.id] ?: return@mapNotNull null
            PerformanceManagementOverviewResponse(
                postId = post.id!!,
                profileImageUrl = post.profileImageUrl,
                lastScheduleTime = lastTime
            )
        }
    }

    @Transactional
    fun deletePerformancePost(postId: Long, user: User) {
        val post = performancePostRepository.findById(postId)
            .orElseThrow { NotFoundException("해당 공연 게시글이 존재하지 않습니다.") }

        if (post.user.id != user.id) {
            throw UnauthorizedAccessException()
        }

        // 1. 프로필 이미지 삭제
        s3Uploader.delete(post.profileImageUrl)

        // 2. 상세 이미지들 삭제
        val detailImages = performanceImageRepository.findByPerformancePost(post)
        detailImages.forEach { s3Uploader.delete(it.imageUrl) }

        // 3. 회차들 조회
        val schedules = performanceScheduleRepository.findByPerformancePost(post)

        // 4. **중요**: 예매 좌석(reservation_seat) 먼저 삭제
        schedules.forEach { schedule ->
            val reservationRequests = reservationRequestRepository.findByPerformanceScheduleIdOrderByRegDateDesc(schedule.id!!)
            reservationRequests.forEach { request ->
                reservationSeatRepository.deleteByReservationRequest(request)
            }
        }

        // 5. 예매 신청(reservation_request) 삭제
        schedules.forEach { schedule ->
            val reservationRequests = reservationRequestRepository.findByPerformanceScheduleIdOrderByRegDateDesc(schedule.id!!)
            reservationRequestRepository.deleteAll(reservationRequests)
        }

        // 6. 스케줄 좌석(schedule_seat) 삭제
        scheduleSeatRepository.deleteAllByPerformanceScheduleIn(schedules)

        // 7. 나머지 하위 연관 엔티티 삭제
        ticketOptionRepository.deleteAllByPerformancePost(post)
        performanceImageRepository.deleteAllByPerformancePost(post)
        performanceScheduleRepository.deleteAllByPerformancePost(post)

        // 8. 최종 게시글 삭제
        performancePostRepository.delete(post)
    }
    @Transactional
    fun updatePerformancePost(
        postId: Long,
        request: PerformancePostUpdateRequest,
        newDetailImages: List<MultipartFile>,
        user: User
    ): Long {
        // 1. 게시글 조회 및 사용자 검증
        val post = performancePostRepository.findById(postId)
            .orElseThrow { NotFoundException("해당 게시글이 존재하지 않습니다.") }

        if (post.user.id != user.id) throw UnauthorizedAccessException()

        // 2. 공연 게시글 기본 정보 수정
        post.updateFromDto(request)

        // 3. 기존 회차 조회
        val existingSchedules = performanceScheduleRepository.findByPerformancePost(post)

        // 4. 기존 좌석 정보 먼저 삭제 (외래키 제약 위반 방지)
        scheduleSeatRepository.deleteAllByPerformanceScheduleIn(existingSchedules)

        // 5. 기존 회차 삭제
        performanceScheduleRepository.deleteAll(existingSchedules)

        // 6. 새 회차 저장 (index → entity 매핑)
        val newSchedules: Map<Int, PerformanceSchedule> = request.schedules
            .mapIndexed { index, scheduleDto ->
                index to performanceScheduleRepository.save(scheduleDto.toEntity(post))
            }
            .toMap()

        // 7. 새 좌석 정보 저장
        request.seatUnavailableCodesPerSchedule.forEach { seatData ->
            val targetSchedule = newSchedules[seatData.scheduleIndex]
                ?: throw IllegalArgumentException("유효하지 않은 scheduleIndex: ${seatData.scheduleIndex}")

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

        // 8. 기존 티켓 옵션 삭제 후 재저장
        ticketOptionRepository.deleteAllByPerformancePost(post)
        request.ticketOptions.forEach {
            ticketOptionRepository.save(it.toEntity(post))
        }

        // 9. 상세 이미지 개수 확인
        val existingDetailImages = performanceImageRepository.findByPerformancePost(post)
        val totalImageCount = existingDetailImages.size + newDetailImages.size
        logger.info("공연 '${post.title}'의 상세 이미지 총 개수: $totalImageCount (기존 ${existingDetailImages.size}, 새로 추가 ${newDetailImages.size})")

        if (totalImageCount > 4) {
            throw IllegalArgumentException("상세 이미지는 최대 4장까지만 등록 가능합니다.")
        }

        // 10. 새 이미지 업로드 및 저장
        val newUrls = s3Uploader.upload(newDetailImages, "camticket/detail")
        newUrls.forEach { imageUrl ->
            performanceImageRepository.save(
                PerformanceImage(imageUrl = imageUrl, performancePost = post)
            )
        }

        return post.id!!
    }



}
