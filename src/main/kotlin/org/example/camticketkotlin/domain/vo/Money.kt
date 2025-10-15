/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.camticketkotlin.domain.vo

/**
 * 금액 Value Object
 *
 * 도메인 규칙:
 * - 금액은 0 이상
 * - 불변 객체
 */
data class Money(val amount: Int) {

    init {
        require(amount >= 0) { "금액은 0 이상이어야 합니다: $amount" }
    }

    operator fun plus(other: Money): Money = Money(this.amount + other.amount)

    operator fun times(quantity: Int): Money {
        require(quantity >= 0) { "수량은 0 이상이어야 합니다: $quantity" }
        return Money(this.amount * quantity)
    }

    fun isZero(): Boolean = amount == 0

    companion object {
        val ZERO = Money(0)

        fun of(amount: Int): Money = Money(amount)
    }
}