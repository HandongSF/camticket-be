package org.example.camticketkotlin.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.camticketkotlin.controller.response.KakaoLoginResponse
import org.example.camticketkotlin.util.JwtUtil
import org.example.camticketkotlin.service.AuthService
import org.example.camticketkotlin.service.KakaoService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.example.camticketkotlin.dto.UserDto

@RestController
class AuthController(
        private val authService: AuthService,
        private val kakaoService: KakaoService,
        private val jwtUtil: JwtUtil,

        @Value("\${custom.jwt.secret}")
        private val secretKey: String,

        @Value("\${custom.jwt.expire-time-ms}")
        private val expireTimeMs: Long,
) {

    @GetMapping("/camticket/auth/kakao-login")
    fun kakaoLogin(
            @RequestParam code: String,
            request: HttpServletRequest,
            response: HttpServletResponse
    ): ResponseEntity<KakaoLoginResponse> {

        val origin = request.getHeader("Origin")
        val redirectUri = "$origin/login/oauth/kakao"

        val userDto: UserDto = authService.kakaoLogin(
                kakaoService.kakaoLogin(code, redirectUri)
        )

        val jwtToken = jwtUtil.createToken(
            userDto.id!!,
            userDto.role,
            secretKey,
            expireTimeMs
        )


        response.setHeader("Authorization", "Bearer ${jwtToken[0]}")

        return ResponseEntity.ok(
                KakaoLoginResponse(
                        name = userDto.name,
                        profileImageUrl = userDto.profileImageUrl,
                        email = userDto.email
                )
        )
    }


}
