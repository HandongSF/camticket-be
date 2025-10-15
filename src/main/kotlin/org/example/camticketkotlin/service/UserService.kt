package org.example.camticketkotlin.service

import org.example.camticketkotlin.domain.User
import org.example.camticketkotlin.dto.UserDto
import org.example.camticketkotlin.dto.request.UserProfileUpdateRequest
import org.example.camticketkotlin.dto.response.ArtistUserOverviewResponse
import org.example.camticketkotlin.exception.NotFoundException
import org.example.camticketkotlin.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.example.camticketkotlin.domain.enums.Role


@Service
class UserService (
    private val userRepository: UserRepository,
    private val s3Uploader: S3Uploader

) {
    companion object {
        private val logger = LoggerFactory.getLogger(UserService::class.java)
    }

    @Transactional
    fun updateUserProfile(user: User, request: UserProfileUpdateRequest) {
        val foundUser = userRepository.findById(user.id!!)
            .orElseThrow { NotFoundException("해당 유저가 존재하지 않습니다.") }

        // 닉네임 중복 검증 (Application Service 레벨 - DB 조회 필요)
        request.nickName?.let { newNickName ->
            if (newNickName.length < 2) {
                throw IllegalArgumentException("닉네임은 최소 2글자 이상이어야 합니다.")
            }

            // 본인의 닉네임이 아닌데 중복되는 경우
            val isDuplicate = userRepository.existsByNickName(newNickName) &&
                    foundUser.nickName != newNickName

            if (isDuplicate) {
                throw IllegalArgumentException("이미 사용 중인 닉네임입니다.")
            }
        }

        // DDD: 도메인 로직 호출 (검증 + 업데이트)
        foundUser.updateProfile(
            newNickName = request.nickName,
            newIntroduction = request.introduction,
            newBankAccount = request.bankAccount
        )

        userRepository.save(foundUser)
    }

    @Transactional
    fun updateProfileImage(user: User, newImage: MultipartFile): String {
        val foundUser = userRepository.findById(user.id!!)
            .orElseThrow { NotFoundException("유저가 존재하지 않습니다.") }

        logger.info("🔍 기존 프로필 이미지 URL: ${foundUser.profileImageUrl}")

        // 기존 이미지 삭제 (Infrastructure 계층)
        foundUser.profileImageUrl?.let {
            logger.info("🗑️ S3에서 기존 이미지 삭제 시도: $it")
            s3Uploader.delete(it)  // S3Uploader 내부에서 prefix 체크 + 예외 처리
        }

        // 새 이미지 업로드 (Infrastructure 계층)
        val uploadedUrl = s3Uploader.upload(newImage, "camticket/user")
        logger.info("✅ 새 프로필 이미지 업로드 완료: $uploadedUrl")

        // DDD: 도메인 로직 호출 (검증 + 업데이트)
        foundUser.updateProfileImage(uploadedUrl)
        userRepository.save(foundUser)

        return uploadedUrl
    }

    @Transactional(readOnly = true)
    fun getUserDtoById(userId: Long): UserDto {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("해당 유저가 존재하지 않습니다.") }

        return UserDto.toDto(user)
    }

    @Transactional(readOnly = true)
    fun getAllManagerUsers(): List<ArtistUserOverviewResponse> {
        return userRepository.findAllByRole(Role.ROLE_MANAGER).map {
            ArtistUserOverviewResponse(
                userId = it.id!!,
                nickName = it.nickName!!,
                profileImageUrl = it.profileImageUrl
            )
        }

    }
}
