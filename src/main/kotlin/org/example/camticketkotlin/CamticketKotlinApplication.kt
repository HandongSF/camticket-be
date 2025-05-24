package org.example.camticketkotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class CamticketKotlinApplication

fun main(args: Array<String>) {
    runApplication<CamticketKotlinApplication>(*args)
}
