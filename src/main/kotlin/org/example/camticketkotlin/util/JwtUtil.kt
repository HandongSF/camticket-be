package org.example.camticket.util;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.camticket.exception.WrongTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Component
public class JwtUtil {

    @Value("${custom.jwt.expire-time-ms}") // JWT 만료 시간을 주입받음
    private long EXPIRE_TIME_MS;
    @Value("${custom.jwt.refresh-expire-time-ms}") // JWT 만료 시간을 주입받음
    private long EXPIRE_REFRESH_TIME_MS;

    // 액세스 토큰만 발급
    public List<String> createToken(Long userId, String secretKey, long expireTimeMs, long unused) {
        Claims claims = Jwts.claims();
        claims.put("userId", userId);

        String accessToken = Jwts.builder()
                .setClaims(claims)
                .claim("tokenType", "ACCESS")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireTimeMs))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        return Arrays.asList(accessToken);
    }

    // JWT에서 userId 추출하는 메서드
    public static Long getUserId(String token, String secretKey) {
            // 토큰에서 Claim을 추출하고 userId를 반환
        return extractClaims(token, secretKey).get("userId", Long.class);
    }

    // SecretKey를 사용해 Token을 검증하고, Claim을 추출하는 메서드
    private static Claims extractClaims(String token, String secretKey) {
        try {
            // 토큰을 파싱하여 Claim을 추출
            return Jwts.parser()
                    .setSigningKey(secretKey)  // 서명 검증을 위해 비밀키 설정
                    .parseClaimsJws(token) // 토큰을 파싱하고 유효성 검사를 수행
                    .getBody(); // 유효한 경우 토큰의 본문(Claim)을 반환
        } catch (ExpiredJwtException e) {
            throw new WrongTokenException("만료된 토큰입니다.");

        }
    }


}
