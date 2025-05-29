import org.example.camticketkotlin.domain.ReservationStatus
import org.example.camticketkotlin.domain.enums.PerformanceCategory
import org.example.camticketkotlin.domain.enums.PerformanceLocation
import org.example.camticketkotlin.dto.response.ReservationDetailResponse
import java.time.LocalDateTime

data class AdminReservationDetailResponse(
    // 예매 기본 정보
    val reservationId: Long,
    val status: ReservationStatus,
    val statusDescription: String,
    // 공연 정보 (관리자는 더 상세한 정보 필요)
    val performanceInfo: AdminPerformanceInfo,
    // 예매자 상세 정보
    val customerInfo: CustomerInfo,
    // 좌석 정보
    val seatInfo: ReservationDetailResponse.SeatInfo,
    // 결제 정보 (배열로 수정)
    val paymentInfo: List<ReservationDetailResponse.PaymentInfoItem>,  // ← 이것만 남기고
) {
    data class AdminPerformanceInfo(
        val performanceId: Long,
        val title: String,
        val category: PerformanceCategory,
        val location: PerformanceLocation,
        val performanceDate: LocalDateTime,
        val scheduleId: Long,
        val scheduleIndex: Int,
        val profileImageUrl: String
    )

    data class CustomerInfo(
        val userId: Long,
        val userName: String,
        val userEmail: String,
        val userNickname: String?,
        val userBankAccount: String
    )

}