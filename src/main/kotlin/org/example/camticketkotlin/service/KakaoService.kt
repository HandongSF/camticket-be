package org.example.camticketkotlin.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.example.camticketkotlin.domain.enums.Role
import org.example.camticketkotlin.dto.UserDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

@Service
class KakaoService(
        @Value("\${kakao.api.key.client}")
        private val clientId: String
) {
    fun kakaoLogin(code: String, redirectUri: String): UserDto {
        val accessToken = getAccessToken(code, redirectUri)
        return getKakaoUserInfo(accessToken)
    }

    private fun getAccessToken(code: String, redirectUri: String): String {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val body: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", clientId)
            add("redirect_uri", redirectUri)
            add("code", code)
        }

        val request = HttpEntity(body, headers)

        val response = RestTemplate().exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                request,
                String::class.java
        )

        val jsonNode: JsonNode = ObjectMapper().readTree(response.body)
        return jsonNode["access_token"].asText()
    }

    private fun getKakaoUserInfo(accessToken: String): UserDto {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val request = HttpEntity<MultiValueMap<String, String>>(headers)

                val response = RestTemplate().exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                request,
                String::class.java
        )

        val jsonNode: JsonNode = ObjectMapper().readTree(response.body)

        val id = jsonNode["id"].asLong()
        val email = jsonNode["kakao_account"]["email"].asText()
        val nickname = jsonNode["properties"]["nickname"].asText()
        val profileImageUrl = jsonNode["properties"]["profile_image"].asText()

        return UserDto(
                kakaoId = id,
                name = nickname,
                email = email,
                profileImageUrl = profileImageUrl,
                introduction = "",
                role = Role.ROLE_USER // 기본 사용자 권한 지정
        )
    }
}
