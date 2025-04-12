package org.example.camticket.config

import org.example.camticket.service.AuthService
import org.example.camticket.filter.ExceptionHandlerFilter
import org.example.camticket.filter.JwtTokenFilter
import org.example.camticket.util.JwtUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
        private val authService: AuthService,
        private val jwtUtil: JwtUtil,
        @Value("\${custom.jwt.secret}")
        private val secretKey: String,
        @Value("#{\${custom.host.client}.split(',')}")
        private val hostClient: List<String>
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
                .cors(Customizer.withDefaults())
                .csrf { it.disable() }
                .formLogin { it.disable() }
                .addFilterBefore(ExceptionHandlerFilter(), UsernamePasswordAuthenticationFilter::class.java)
                .addFilterBefore(JwtTokenFilter(authService, secretKey, jwtUtil), UsernamePasswordAuthenticationFilter::class.java)
                .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
                .authorizeHttpRequests {
                    it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers("/camticket/auth/**", "/error", "/").permitAll()
                            .requestMatchers("/camticket/every/**").permitAll()
                            .requestMatchers("/camticket/api/**").authenticated()
                }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = hostClient
            allowedMethods = listOf("POST", "GET", "PATCH", "DELETE", "PUT")
            allowedHeaders = listOf(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "X-Refresh-Token")
            exposedHeaders = listOf(HttpHeaders.AUTHORIZATION, "X-Refresh-Token")
            allowCredentials = true
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
