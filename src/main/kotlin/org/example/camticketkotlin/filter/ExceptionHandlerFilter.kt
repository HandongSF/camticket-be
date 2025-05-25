package org.example.camticketkotlin.filter


import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.camticketkotlin.controller.response.ExceptionResponse
import org.example.camticketkotlin.exception.WrongTokenException
import org.example.camticketkotlin.exception.DoNotLoginException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.nio.charset.StandardCharsets

class ExceptionHandlerFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (e: DoNotLoginException) {
            setErrorResponse(response, e.message ?: "로그인하지 않은 사용자입니다.", HttpStatus.UNAUTHORIZED)
        } catch (e: WrongTokenException) {
            setErrorResponse(response, e.message ?: "잘못된 토큰입니다.", HttpStatus.UNAUTHORIZED)
        } catch (e: IllegalArgumentException) {
            setErrorResponse(response, e.message ?: "요청이 잘못되었습니다.", HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            setErrorResponse(response, "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun setErrorResponse(response: HttpServletResponse, message: String, httpStatus: HttpStatus) {
        val objectMapper = ObjectMapper()
        response.status = httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()

        val exceptionResponse = ExceptionResponse(
            error = httpStatus.reasonPhrase,
            message = message
        )

        try {
            response.writer.write(objectMapper.writeValueAsString(exceptionResponse))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

