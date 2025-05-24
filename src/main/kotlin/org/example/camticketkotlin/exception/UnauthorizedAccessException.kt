package org.example.camticketkotlin.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.FORBIDDEN)
class UnauthorizedAccessException : RuntimeException("접근 권한이 없습니다.")
