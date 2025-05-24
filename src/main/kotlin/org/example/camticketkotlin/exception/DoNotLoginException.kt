package org.example.camticketkotlin.exception;

// Kotlin에서 :는 **"타입이다" 혹은 "상속한다"**는 뜻
class DoNotLoginException : RuntimeException("로그인하지 않은 사용자입니다.")
