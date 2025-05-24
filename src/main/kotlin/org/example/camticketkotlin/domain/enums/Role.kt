package org.example.camticketkotlin.domain.enums

enum class Role {
    ROLE_USER,
    ROLE_MANAGER, // 동아리 관리자
    ROLE_ADMIN;

    fun getKey(): String = name
}
