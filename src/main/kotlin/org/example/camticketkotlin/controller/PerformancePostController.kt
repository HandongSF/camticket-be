package org.example.camticketkotlin.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.example.camticketkotlin.dto.response.ArtistPerformanceSummaryResponse
import org.example.camticketkotlin.dto.response.PerformancePostOverviewResponse
import org.example.camticketkotlin.service.PerformancePostService
import org.example.camticketkotlin.common.ApiResponse as ApiWrapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/camticket/api/performance")
class PerformancePostController(
    private val performancePostService: PerformancePostService
) {

    @Operation(summary = "공연 게시글 오버뷰 전체 조회",
        description = "전체 공연의 요약 정보를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "공연 오버뷰 조회 성공"),
            ApiResponse(responseCode = "500", description = "서버 오류")
        ]
    )
    @GetMapping("/overview")
    fun getAllPerformances(): ResponseEntity<ApiWrapper<List<PerformancePostOverviewResponse>>> {
        val result = performancePostService.getAllPerformancesOverview()
        return ResponseEntity.ok(ApiWrapper.success(result, "공연 목록을 성공적으로 조회했습니다."))
    }

    @Operation(summary = "특정 유저의 공연 요약 정보 조회", description = "userId를 기준으로 해당 사용자가 업로드한 공연의 ID와 프로필 이미지를 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공적으로 아티스트 공연 목록을 조회했습니다."),
            ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            ApiResponse(responseCode = "500", description = "서버 오류입니다.")
        ]
    )
    @GetMapping("/profile/{userId}")
    fun getArtistPerformances(
        @PathVariable
        @Parameter(description = "아티스트 페이지에서 아티스트를 눌렀을 때 나오는 프로필 사진들", example = "1")
        userId: Long
    ): ResponseEntity<ApiWrapper<List<ArtistPerformanceSummaryResponse>>> {
        val result = performancePostService.getPerformanceSummariesByUserId(userId)
        return ResponseEntity.ok(ApiWrapper.success(result, "아티스트의 공연 목록을 조회했습니다."))
    }
}
