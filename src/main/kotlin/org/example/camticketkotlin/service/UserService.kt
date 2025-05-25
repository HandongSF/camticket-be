package org.example.camticketkotlin.service

import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.dto.request.UserProfileUpdateRequest
import org.example.camticketkotlin.exception.NotFoundException
import org.example.camticketkotlin.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService (
    private val userRepository: UserRepository
){
    @Transactional
    fun updateUserProfile(user: User, request: UserProfileUpdateRequest) {
        val foundUser = userRepository.findById(user.id!!)
            .orElseThrow { NotFoundException("해당 유저가 존재하지 않습니다.") }

        request.nickName?.let { foundUser.nickName = it }
        request.introduction?.let { foundUser.introduction = it }
    }


}
