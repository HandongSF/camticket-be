package org.example.camticketkotlin.dto.request

import org.example.camticketkotlin.domain.PerformancePost
import org.example.camticketkotlin.domain.TicketOption

data class TicketOptionRequest(
    val name: String,
    val price: Int
) {
    fun toEntity(post: PerformancePost): TicketOption {
        return TicketOption(
            name = this.name,
            price = this.price,
            performancePost = post
        )
    }
}
