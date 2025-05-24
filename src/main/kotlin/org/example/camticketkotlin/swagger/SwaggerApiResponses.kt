package org.example.camticketkotlin.swagger

import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
    value = [
        ApiResponse(responseCode = "201", description = "게시글이 성공적으로 생성됨"),
        ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        ApiResponse(
            responseCode = "401",
            description = "인증 필요 또는 토큰 만료"
        )
    ]
)
annotation class SwaggerCreatePerformancePostResponses
