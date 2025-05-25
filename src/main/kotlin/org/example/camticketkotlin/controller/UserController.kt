package org.example.camticketkotlin.controller

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.dto.request.UserProfileUpdateRequest
import org.example.camticketkotlin.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
        @Parameter(hidden = true)
        @AuthenticationPrincipal user: User,
        @RequestBody request: UserProfileUpdateRequest
    ): ResponseEntity<ApiWrapper<Unit>> {
        userService.updateUserProfile(user, request)
        return ResponseEntity
            .status(HttpStatus.OK) // 200 OK
            .body(ApiWrapper.success(Unit, "프로필이 성공적으로 수정되었습니다."))
    }


}