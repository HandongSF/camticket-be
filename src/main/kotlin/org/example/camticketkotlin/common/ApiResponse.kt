package org.example.camticketkotlin.common

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun success(message: String = "성공"): ApiResponse<Unit> =
            ApiResponse(200, message, null)

        fun <T> success(data: T, message: String = "성공"): ApiResponse<T> =
            ApiResponse(200, message, data)

        fun created(message: String = "생성되었습니다."): ApiResponse<Unit> =
            ApiResponse(201, message, null)

        fun <T> created(message: String = "생성되었습니다.", data: T): ApiResponse<T> =
            ApiResponse(201, message, data)

        fun error(code: Int = 400, message: String): ApiResponse<Unit> =
            ApiResponse(code, message, null)
    }
}
