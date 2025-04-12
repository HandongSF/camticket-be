package org.example.camticket.service;

import lombok.RequiredArgsConstructor;
import org.example.camticket.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class RandomNicknameService {

    private final UserRepository userRepository;
    private final Random random = new Random();

    private final String[] COLORS = {
            "빨간", "주황", "노란", "초록", "파란", "남색", "보라", "하얀", "검정", "회색", "분홍", "청록",
            "연두", "자주", "카키", "베이지", "은색", "금색", "파스텔", "네이비", "민트", "살구", "크림", "밤색"
    };

    private final String[] FOODS = {
            "피자", "초코칩", "감귤", "파스타", "초밥", "떡볶이", "김밥", "햄버거", "붕어빵", "치즈볼", "핫도그", "샌드위치",
            "컵라면", "딸기우유", "오렌지", "카레", "닭강정", "베이컨", "아이스크림", "스테이크", "크로플", "에그타르트", "마카롱", "케이크"
    };

    public String generateUniqueNickname() {
        String nickname;
        int attempts = 0;

        do {
            String color = COLORS[random.nextInt(COLORS.length)];
            String food = FOODS[random.nextInt(FOODS.length)];
            int number = 10 + random.nextInt(90); // 10 ~ 99 사이 숫자
            nickname = color + " " + food  + number;
            attempts++;
            if (attempts > 1000) {
                throw new RuntimeException("닉네임 생성 시도 횟수 초과");
            }
        } while (userRepository.existsByNickName(nickname));

        return nickname;
    }
}
