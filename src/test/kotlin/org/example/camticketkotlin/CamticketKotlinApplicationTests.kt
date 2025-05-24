package org.example.camticketkotlin

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest

@SpringBootTest
class CamticketKotlinApplicationTests {

    @Test
    fun printAwsCallerArn() {
        val stsClient = StsClient.create()
        val response = stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build())
        println("🔐 현재 AWS ARN: ${response.arn()}")
    }
}
