package org.example.camticketkotlin.controller

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.dto.request.UserProfileUpdateRequest
import org.example.camticketkotlin.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.example.camticketkotlin.common.ApiResponse as ApiWrapper

@RestController
@RequestMapping("/camticket/api/user")
class UserController (
    private val userService: UserService
){
    @PatchMapping("/profile")
    @ApiResponse(responseCode = "200", description = "프로필 수정 성공")
    @Operation(summary = "사용자 프로필 수정", description = "닉네임, 소개글을 수정합니다.")
    fun updateProfile(
        @AuthenticationPrincipal user: User,
        @RequestBody request: UserProfileUpdateRequest
    ): ResponseEntity<ApiWrapper<Long>> {
        userService.updateUserProfile(user, request)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiWrapper.success(user.id!!, "프로필이 성공적으로 수정되었습니다."))
    }

    @PatchMapping("/profile/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ApiResponse(responseCode = "200", description = "프로필 이미지 수정 성공")
    @Operation(summary = "프로필 이미지 수정", description = "사용자의 프로필 이미지를 변경합니다.")
    fun updateProfileImage(
        @AuthenticationPrincipal user: User,
        @RequestPart("image") image: MultipartFile
    ): ResponseEntity<ApiWrapper<String>> {
        val imageUrl = userService.updateProfileImage(user, image)
        return ResponseEntity.ok(ApiWrapper.success(imageUrl, "프로필 이미지가 성공적으로 수정되었습니다."))
    }

}