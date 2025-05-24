package org.example.camticketkotlin.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.example.camticketkotlin.common.ApiResponse as ApiWrapper
import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.dto.request.PerformancePostCreateRequest
import org.example.camticketkotlin.service.PerformancePostService
import org.example.camticketkotlin.swagger.SwaggerCreatePerformancePostResponses
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/camticket/api")
class PerformancePostController(
    private val performancePostService: PerformancePostService
) {

    @Operation(
        summary = "공연 게시글 생성",
        description = "프로필 이미지는 필수이며, 상세 이미지는 선택입니다."
    )
    @SwaggerCreatePerformancePostResponses
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createPerformancePost(
        @RequestPart("request")
        @Parameter(description = "공연 정보 JSON")
        request: PerformancePostCreateRequest,

        @RequestPart("profileImage")
        @Parameter(description = "필수: 프로필 이미지", required = true)
        profileImage: MultipartFile,

        @RequestPart("detailImages", required = false)
        @Parameter(description = "선택: 상세 이미지 (0개 이상 가능)", required = false)
        detailImages: List<MultipartFile> = emptyList(),

        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiWrapper<Long>> {
        val postId = performancePostService
            .createPerformancePost(request, profileImage, detailImages, user)
            .id!!

        return ResponseEntity
            .status(201)
            .body(ApiWrapper.created("공연 게시글이 등록되었습니다.", postId))
    }
}
