package org.example.camticketkotlin.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.example.camticketkotlin.dto.response.PerformancePostOverviewResponse
import org.example.camticketkotlin.service.PerformancePostService
import org.example.camticketkotlin.common.ApiResponse as ApiWrapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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
}
