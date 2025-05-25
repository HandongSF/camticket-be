package org.example.camticketkotlin.controller.advice

import io.swagger.v3.oas.annotations.Hidden
import org.example.camticketkotlin.controller.response.ExceptionResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
@Hidden
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ExceptionResponse> {
        val response = ExceptionResponse(
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = e.message ?: "잘못된 요청입니다." // null 일 경우 잘못된 요청입니다로 표현
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(e: Exception): ResponseEntity<ExceptionResponse> {
        e.printStackTrace()  // 💥 로그 확인용
        val response = ExceptionResponse(
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = e.message ?: "서버 내부 오류가 발생했습니다."
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}
