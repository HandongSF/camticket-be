package org.example.camticketkotlin.service

import org.example.camticketkotlin.repository.UserRepository
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class RandomNicknameService(
        private val userRepository: UserRepository
) {
    private val colors = listOf(
            "빨간", "주황", "노란", "초록", "파란", "남색", "보라", "하얀", "검정", "회색", "분홍", "청록",
            "연두", "자주", "카키", "베이지", "은색", "금색", "파스텔", "네이비", "민트", "살구", "크림", "밤색"
    )

    private val foods = listOf(
            "피자", "초코칩", "감귤", "파스타", "초밥", "떡볶이", "김밥", "햄버거", "붕어빵", "치즈볼", "핫도그", "샌드위치",
            "컵라면", "딸기우유", "오렌지", "카레", "닭강정", "베이컨", "아이스크림", "스테이크", "크로플", "에그타르트", "마카롱", "케이크"
    )

    fun generateUniqueNickname(): String {
        repeat(1000) {
            val color = colors.random()
            val food = foods.random()
            val number = Random.nextInt(10, 100) // 10 ~ 99
            val nickname = "$color $food$number"

            if (!userRepository.existsByNickName(nickname)) {
                return nickname
            }
        }
        throw RuntimeException("닉네임 생성 시도 횟수 초과")
    }
}
