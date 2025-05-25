package org.example.camticketkotlin.repository;

import org.example.camticketkotlin.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.example.camticketkotlin.domain.enums.Role


import java.util.Optional;

interface UserRepository : JpaRepository<User, Long> {
    fun findByKakaoId(kakaoId: Long) : Optional<User>
    fun existsByNickName(nickName: String): Boolean
    fun findAllByRole(role: Role): List<User>


}
