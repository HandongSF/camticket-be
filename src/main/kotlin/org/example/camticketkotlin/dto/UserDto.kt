package org.example.camticket.dto;


import lombok.Builder;
import lombok.Getter;
import org.example.camticket.domain.User;

import java.util.ArrayList;
@Getter
@Builder
public class UserDto {
    private Long id;
    private Long kakaoId;
    private String name;
    private String nickName;
    private String email;
    private String profileImageUrl;


    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .kakaoId(user.getKakaoId())
                .name(user.getName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    public static ArrayList<UserDto> from(ArrayList<User> users) {
        ArrayList<UserDto> userDtos = new ArrayList<>();
        for (User user : users) {
            userDtos.add(UserDto.builder()
                    .id(user.getId())
                    .kakaoId(user.getKakaoId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .profileImageUrl(user.getProfileImageUrl())
                    .build());
        }

        return userDtos;
    }
}
