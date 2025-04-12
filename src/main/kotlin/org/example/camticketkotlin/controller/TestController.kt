package org.example.camticket.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController {

    @GetMapping("/camticket/api/test")
    fun testApi(request: HttpServletRequest): String {
        println("==========받은 헤더 , 즉 기존에 있던 액세스, 리프레쉬 토큰")

        val targetHeaders = listOf(HttpHeaders.AUTHORIZATION, "X-Refresh-Token")

        targetHeaders.forEach { headerName ->
                val headerValue = request.getHeader(headerName)
            if (headerValue != null) {
                println("$headerName: $headerValue")
            } else {
                println("$headerName: [헤더 값 없음]")
            }
        }

        return "test api success - check server logs for specific headers"
    }

    @GetMapping("/camticket/every")
    fun everyApi(): String {
        println("every api 요청 호출")
        return "every api success"
    }
}
