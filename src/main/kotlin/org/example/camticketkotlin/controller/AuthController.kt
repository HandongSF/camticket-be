package org.example.camticket.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.camticket.controller.response.KakaoLoginResponse
import org.example.camticket.dto.UserDto
import org.example.camticket.service.AuthService
import org.example.camticket.service.KakaoService
import org.example.camticket.util.JwtUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
        private val authService: AuthService,
        private val kakaoService: KakaoService,
        private val jwtUtil: JwtUtil,

        @Value("\${custom.jwt.secret}")
        private val secretKey: String,

        @Value("\${custom.jwt.expire-time-ms}")
        private val expireTimeMs: Long,

        @Value("\${custom.jwt.refresh-expire-time-ms}")
        private val expireRefreshTimeMs: Long
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
                userDto.id,
                secretKey,
                expireTimeMs,
                expireRefreshTimeMs
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
