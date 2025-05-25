package org.example.camticketkotlin.util

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.example.camticketkotlin.domain.enums.Role
import org.example.camticketkotlin.exception.WrongTokenException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JwtUtil {

    @Value("\${custom.jwt.expire-time-ms}")
    private val expireTimeMs: Long = 0

    @Value("\${custom.jwt.refresh-expire-time-ms}")
    private val expireRefreshTimeMs: Long = 0

    fun createToken(userId: Long, role: Role, secretKey: String, expireTimeMs: Long): List<String> {
        val claims: Claims = Jwts.claims().apply {
            this["userId"] = userId
            this["role"] = role.name // 여기서 오류 해결됨
        }

        val accessToken = Jwts.builder()
            .setClaims(claims)
            .claim("tokenType", "ACCESS")
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + expireTimeMs))
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact()

        return listOf(accessToken)
    }


    companion object {
        fun getUserId(token: String, secretKey: String): Long {
            val raw = extractClaims(token, secretKey)["userId"]
                ?: throw WrongTokenException("JWT에 userId가 없습니다.")

            return when (raw) {
                is Int -> raw.toLong()
                is Long -> raw
                is Number -> raw.toLong()
                else -> throw WrongTokenException("userId가 올바른 숫자 형식이 아닙니다.")
            }
        }


        private fun extractClaims(token: String, secretKey: String): Claims {
            return try {
                Jwts.parser()
                        .setSigningKey(secretKey)
                        .parseClaimsJws(token)
                        .body
            } catch (e: ExpiredJwtException) {
                throw WrongTokenException("만료된 토큰입니다.")
            }
        }
    }
}
