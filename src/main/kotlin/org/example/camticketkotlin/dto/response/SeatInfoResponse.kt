// 3. 좌석 정보 응답 DTO  
package org.example.camticketkotlin.dto.response

import org.example.camticketkotlin.domain.SeatStatus

data class SeatInfoResponse(
    val seatCode: String,         // 좌석 코드 (A1, B3 등)
    val status: SeatStatus,       // AVAILABLE, UNAVAILABLE, RESERVED
    val isSelected: Boolean = false  // 클라이언트에서 선택 상태 관리용
)
