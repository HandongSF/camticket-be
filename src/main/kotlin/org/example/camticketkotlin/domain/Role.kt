package org.example.camticket.domain

enum class Role {
    ROLE_USER,
    ROLE_MANAGER, // 동아리 관리자
    ROLE_ADMIN;

    fun getKey(): String = name
}
