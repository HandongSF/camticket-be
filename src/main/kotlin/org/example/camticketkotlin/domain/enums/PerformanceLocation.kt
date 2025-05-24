package org.example.camticketkotlin.domain.enums

enum class PerformanceLocation(val displayName: String) {
    HAKGWAN_104("학관 104호"),
    BUSAN("BUSAN"),
    INCHEON("INCHEON"),
    ETC("기타");

    override fun toString(): String = displayName
}
