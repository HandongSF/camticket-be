// 4. 티켓 옵션 응답 DTO
package org.example.camticketkotlin.dto.response

data class TicketOptionResponse(
    val optionId: Long,
    val name: String,           // 일반, 새내기 등
    val price: Int,            // 가격
    val availableCount: Int    // 해당 옵션으로 예매 가능한 수량
)