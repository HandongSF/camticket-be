package org.example.camticketkotlin.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement as SwaggerSecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme as SwaggerSecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@SecurityScheme(
    name = "JWT",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("CamTicket API")
                    .description("캠티켓 API 문서입니다.")
                    .version("v1")
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "JWT",
                        SwaggerSecurityScheme()
                            .type(SwaggerSecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
            .addSecurityItem(
                SwaggerSecurityRequirement().addList("JWT")
            )
    }
}
