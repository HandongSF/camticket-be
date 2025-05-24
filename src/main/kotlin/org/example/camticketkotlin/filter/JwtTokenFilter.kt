package org.example.camticketkotlin.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.exception.WrongTokenException
import org.example.camticketkotlin.service.AuthService
import org.example.camticketkotlin.util.JwtUtil
import org.example.camticketkotlin.exception.DoNotLoginException
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(JwtTokenFilter::class.java)

class JwtTokenFilter(
        private val authService: AuthService,
        private val secretKey: String,
        private val jwtUtil: JwtUtil
) : OncePerRequestFilter() {

    override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
    ) {
        val uri = request.requestURI

        if (
            uri.startsWith("/error") ||
            uri.startsWith("/swagger-ui") ||
            uri.startsWith("/v3/api-docs") ||
            uri.startsWith("/swagger-resources") || // (혹시 사용하는 경우)
            uri.startsWith("/webjars") ||           // (정적 리소스)
            uri.startsWith("/camticket/auth/") ||
            uri.startsWith("/camticket/every") ||
            uri == "/"
        ) {
            filterChain.doFilter(request, response)
            return
        }


        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        logger.debug("Authorization header: $authorizationHeader")

        if (authorizationHeader == null) {
            val paramToken = request.getParameter("token")
            if (paramToken == null) {
                throw DoNotLoginException()
            }
            processAccessToken(request, response, filterChain, paramToken)
            return
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            logger.warn("잘못된 토큰 형식: $authorizationHeader")
            throw WrongTokenException("Bearer 로 시작하지 않는 토큰입니다.")
        }

        val token = authorizationHeader.split(" ")[1]
        processAccessToken(request, response, filterChain, token)
    }

    private fun processAccessToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
        token: String
    ) {
        try {
            logger.debug("Access token 수신: $token")
            val userId = JwtUtil.getUserId(token, secretKey)
            logger.debug("토큰에서 추출한 userId: $userId")

            val loginUser = authService.getLoginUser(userId)
            logger.debug("DB에서 조회된 사용자: ${loginUser.id}, ${loginUser.name}")

            setAuthenticationForUser(request, loginUser)
        } catch (ex: Exception) {
            logger.warn("JWT 인증 실패: ${ex.message}", ex)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.")
            return
        }

        filterChain.doFilter(request, response)
    }


    private fun setAuthenticationForUser(request: HttpServletRequest, user: User) {
        logger.debug("SecurityContextHolder에 인증 정보 설정: userId=${user.id}, role=${user.role}")
        val authenticationToken = UsernamePasswordAuthenticationToken(
                user,
                null,
                listOf(SimpleGrantedAuthority(user.role.name))
        )
        authenticationToken.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authenticationToken
    }
}
