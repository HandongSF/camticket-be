package org.example.camticketkotlin.dto.request

data class ReservationCreateRequest(
    val performancePostId: Long,      // 공연 게시글 ID 추가 (안전성)
    val performanceScheduleId: Long,  // 선택한 공연 회차 ID
    val ticketOptionId: Long,         // 선택한 티켓 옵션 ID
    val count: Int,                   // 예매 수량
    val selectedSeatCodes: List<String> = emptyList(), // 선택한 좌석 코드들 (지정석인 경우)

    // 추가 검증 필드들
    val expectedTotalPrice: Int,      // 클라이언트에서 계산한 총 가격 (검증용)
    val performanceTitle: String      // 공연 제목 (확인용)
)