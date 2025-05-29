package org.example.camticketkotlin.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.example.camticketkotlin.common.ApiResponse as ApiWrapper
import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.dto.request.ReservationCreateRequest
import org.example.camticketkotlin.dto.response.*
import org.example.camticketkotlin.service.ReservationService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/camticket/api/reservation")
class ReservationController(
    private val reservationService: ReservationService
) {

    @Operation(
        summary = "공연 회차 목록 조회",
        description = "특정 공연의 회차 목록과 각 회차별 예매 가능 상태를 조회합니다."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "회차 목록 조회 성공"),
        ApiResponse(responseCode = "404", description = "해당 공연이 존재하지 않음")
    ])
    @GetMapping("/schedules/{postId}")
    fun getPerformanceSchedules(
        @PathVariable
        @Parameter(description = "공연 게시글 ID")
        postId: Long
    ): ResponseEntity<ApiWrapper<List<PerformanceScheduleResponse>>> {
        val schedules = reservationService.getPerformanceSchedules(postId)
        return ResponseEntity.ok(ApiWrapper.success(schedules, "공연 회차 목록을 조회했습니다."))
    }

    @Operation(
        summary = "좌석 정보 조회",
        description = "특정 공연 회차의 좌석 배치와 예매 상태를 조회합니다. (지정석인 경우에만)"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "좌석 정보 조회 성공"),
        ApiResponse(responseCode = "404", description = "해당 공연 회차가 존재하지 않음")
    ])
    @GetMapping("/seats/{scheduleId}")
    fun getSeatInfo(
        @PathVariable
        @Parameter(description = "공연 회차 ID")
        scheduleId: Long
    ): ResponseEntity<ApiWrapper<List<SeatInfoResponse>>> {
        val seats = reservationService.getSeatInfo(scheduleId)
        return ResponseEntity.ok(ApiWrapper.success(seats, "좌석 정보를 조회했습니다."))
    }

    @Operation(
        summary = "티켓 옵션 조회",
        description = "특정 공연의 티켓 옵션들(가격, 등급)을 조회합니다."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "티켓 옵션 조회 성공"),
        ApiResponse(responseCode = "404", description = "해당 공연이 존재하지 않음")
    ])
    @GetMapping("/ticket-options/{postId}")
    fun getTicketOptions(
        @PathVariable
        @Parameter(description = "공연 게시글 ID")
        postId: Long
    ): ResponseEntity<ApiWrapper<List<TicketOptionResponse>>> {
        val options = reservationService.getTicketOptions(postId)
        return ResponseEntity.ok(ApiWrapper.success(options, "티켓 옵션을 조회했습니다."))
    }

    @Operation(
        summary = "예매 가능 여부 확인",
        description = "사용자가 특정 공연에 예매 가능한지 확인합니다. (예매 기간, 수량 제한 등)"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "예매 가능 여부 확인 완료"),
        ApiResponse(responseCode = "404", description = "해당 공연이 존재하지 않음")
    ])
    @GetMapping("/availability/{postId}")
    fun checkReservationAvailability(
        @PathVariable
        @Parameter(description = "공연 게시글 ID")
        postId: Long,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiWrapper<ReservationAvailabilityResponse>> {
        val availability = reservationService.checkReservationAvailability(user, postId)
        return ResponseEntity.ok(ApiWrapper.success(availability, "예매 가능 여부를 확인했습니다."))
    }

    @Operation(
        summary = "예매 신청",
        description = "사용자가 공연을 예매 신청합니다. 회차, 좌석, 티켓 옵션을 선택하여 신청합니다."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "예매 신청 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 예매 정보"),
        ApiResponse(responseCode = "404", description = "해당 공연 또는 옵션이 존재하지 않음")
    ])
    @PostMapping
    fun createReservation(
        @RequestBody
        @Parameter(description = "예매 신청 정보")
        request: ReservationCreateRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiWrapper<ReservationResponse>> {
        val reservation = reservationService.createReservation(user, request)
        return ResponseEntity
            .status(201)
            .body(ApiWrapper.created("예매 신청이 완료되었습니다.", reservation))
    }

    @Operation(
        summary = "사용자 예매 내역 조회",
        description = "로그인한 사용자의 모든 예매 내역을 조회합니다."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "예매 내역 조회 성공")
    ])
    @GetMapping("/my-reservations")
    fun getUserReservations(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiWrapper<List<ReservationResponse>>> {
        val reservations = reservationService.getUserReservations(user)
        return ResponseEntity.ok(ApiWrapper.success(reservations, "예매 내역을 조회했습니다."))
    }

    @Operation(
        summary = "예매 신청 취소",
        description = "예매 신청을 취소합니다. (PENDING 상태인 경우에만 가능)"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "예매 취소 성공"),
        ApiResponse(responseCode = "400", description = "취소할 수 없는 예매 상태"),
        ApiResponse(responseCode = "403", description = "취소 권한 없음"),
        ApiResponse(responseCode = "404", description = "해당 예매가 존재하지 않음")
    ])
    @DeleteMapping("/{reservationId}")
    fun cancelReservation(
        @PathVariable
        @Parameter(description = "예매 신청 ID")
        reservationId: Long,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiWrapper<Unit>> {
        reservationService.cancelReservation(user, reservationId)
        return ResponseEntity.ok(ApiWrapper.success("예매가 취소되었습니다."))
    }

    @Operation(
        summary = "내 공연의 예매 신청 목록 조회 (관리자용)",
        description = "로그인한 사용자가 등록한 모든 공연에 대한 예매 신청 목록을 조회합니다."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "예매 신청 목록 조회 성공")
    ])
    @GetMapping("/management/my-performances")
    fun getMyPerformanceReservations(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiWrapper<List<ReservationManagementResponse>>> {
        val reservations = reservationService.getReservationRequestsForMyPerformances(user)
        return ResponseEntity.ok(ApiWrapper.success(reservations, "내 공연의 예매 신청 목록을 조회했습니다."))
    }

    @Operation(
        summary = "특정 공연의 예매 신청 목록 조회 (관리자용)",
        description = "특정 공연에 대한 모든 예매 신청 목록을 조회합니다."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "예매 신청 목록 조회 성공"),
        ApiResponse(responseCode = "403", description = "해당 공연의 조회 권한 없음"),
        ApiResponse(responseCode = "404", description = "해당 공연이 존재하지 않음")
    ])
    @GetMapping("/management/performance/{postId}")
    fun getPerformanceReservations(
        @PathVariable
        @Parameter(description = "공연 게시글 ID")
        postId: Long,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiWrapper<List<ReservationManagementResponse>>> {
        val reservations = reservationService.getReservationRequestsForPerformance(user, postId)
        return ResponseEntity.ok(ApiWrapper.success(reservations, "공연의 예매 신청 목록을 조회했습니다."))
    }

    @Operation(
        summary = "예매 상태 변경 (관리자용)",
        description = "예매 신청의 상태를 변경합니다. (PENDING → APPROVED/REJECTED)"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "예매 상태 변경 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 상태 또는 변경 불가능한 예매"),
        ApiResponse(responseCode = "403", description = "권한 없음"),
        ApiResponse(responseCode = "404", description = "해당 예매가 존재하지 않음")
    ])
    @PatchMapping("/{reservationId}/status")
    fun updateReservationStatus(
        @PathVariable
        @Parameter(description = "예매 신청 ID")
        reservationId: Long,
        @RequestParam
        @Parameter(description = "변경할 상태 (APPROVED, REJECTED)")
        status: String,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiWrapper<Unit>> {
        reservationService.updateReservationStatus(user, reservationId, status)
        return ResponseEntity.ok(ApiWrapper.success("예매 상태가 변경되었습니다."))
    }
}