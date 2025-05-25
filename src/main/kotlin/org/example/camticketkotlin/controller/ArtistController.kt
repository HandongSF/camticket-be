package org.example.camticketkotlin.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.example.camticketkotlin.dto.response.ArtistPerformanceSummaryResponse
import org.example.camticketkotlin.service.ArtistService
import org.example.camticketkotlin.common.ApiResponse as ApiWrapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/camticket/api/artist")
class ArtistController(
    private val artistService: ArtistService
) {

    @Operation(summary = "특정 유저의 공연 요약 정보 조회", description = "userId를 기준으로 해당 사용자가 업로드한 공연의 ID와 프로필 이미지를 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공적으로 아티스트 공연 목록을 조회했습니다."),
            ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            ApiResponse(responseCode = "500", description = "서버 오류입니다.")
        ]
    )
    @GetMapping("/detail/{userId}")
    fun getArtistPerformances(
        @PathVariable
        @Parameter(description = "조회할 아티스트의 유저 ID", example = "1")
        userId: Long
    ): ResponseEntity<ApiWrapper<List<ArtistPerformanceSummaryResponse>>> {
        val result = artistService.getPerformanceSummariesByUserId(userId)
        return ResponseEntity.ok(ApiWrapper.success(result, "아티스트의 공연 목록을 조회했습니다."))
    }
}
