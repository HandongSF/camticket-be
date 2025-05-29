package org.example.camticketkotlin.repository

import org.example.camticketkotlin.domain.ReservationSeat
import org.example.camticketkotlin.domain.ReservationRequest
import org.springframework.data.jpa.repository.JpaRepository

interface ReservationSeatRepository : JpaRepository<ReservationSeat, Long> {

    // 특정 예매 신청의 좌석 조회
    fun findByReservationRequest(reservationRequest: ReservationRequest): List<ReservationSeat>

    // 예매 신청 삭제 시 관련 좌석도 삭제
    fun deleteByReservationRequest(reservationRequest: ReservationRequest)
}