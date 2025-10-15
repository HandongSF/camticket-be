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
 * 좌석 코드 Value Object
 *
 * 도메인 규칙:
 * - 좌석 코드는 1~3자의 대문자와 숫자 조합 (예: A1, B12, VIP3)
 * - 불변 객체로 동등성은 값으로 비교
 */
data class SeatCode(val value: String) {

    init {
        require(value.isNotBlank()) { "좌석 코드는 빈 값일 수 없습니다." }
        require(value.matches(Regex("^[A-Z]{1,3}\\d{1,3}$"))) {
            "좌석 코드 형식이 올바르지 않습니다: $value (예: A1, B12, VIP3)"
        }
    }

    override fun toString(): String = value
}