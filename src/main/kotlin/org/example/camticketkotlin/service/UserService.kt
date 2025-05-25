package org.example.camticketkotlin.service

import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.dto.UserDto
import org.example.camticketkotlin.dto.request.UserProfileUpdateRequest
import org.example.camticketkotlin.exception.NotFoundException
import org.example.camticketkotlin.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class UserService (
    private val userRepository: UserRepository,
    private val s3Uploader: S3Uploader

)
{
    companion object {
        private val logger = LoggerFactory.getLogger(UserService::class.java)
    }

    @Transactional
    fun updateUserProfile(user: User, request: UserProfileUpdateRequest) {
        val foundUser = userRepository.findById(user.id!!)
            .orElseThrow { NotFoundException("해당 유저가 존재하지 않습니다.") }

        request.nickName?.let { foundUser.nickName = it }
        request.introduction?.let { foundUser.introduction = it }
    }


    @Transactional
    fun updateProfileImage(user: User, newImage: MultipartFile): String {
        val foundUser = userRepository.findById(user.id!!)
            .orElseThrow { NotFoundException("유저가 존재하지 않습니다.") }

        logger.info("🔍 기존 프로필 이미지 URL: ${foundUser.profileImageUrl}")

        foundUser.profileImageUrl?.let {
            logger.info("🗑️ S3에서 기존 이미지 삭제 시도: $it")
            s3Uploader.delete(it)  // S3Uploader 내부에서 prefix 체크 + 예외 처리
        }

        val uploadedUrl = s3Uploader.upload(newImage, "camticket/user")
        logger.info("✅ 새 프로필 이미지 업로드 완료: $uploadedUrl")

        foundUser.profileImageUrl = uploadedUrl
        return uploadedUrl
    }

    @Transactional(readOnly = true)
    fun getUserDtoById(userId: Long): UserDto {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("해당 유저가 존재하지 않습니다.") }

        return UserDto.toDto(user)
    }


}
