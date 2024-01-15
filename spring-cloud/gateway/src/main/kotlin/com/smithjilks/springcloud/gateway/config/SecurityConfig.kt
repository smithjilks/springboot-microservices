package com.smithjilks.springcloud.gateway.config

import mu.KLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain


@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    companion object : KLogging()

    @Bean
    @Throws(Exception::class)
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/headerrouting/**").permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/eureka/**").permitAll()
                    .pathMatchers("/oauth2/**").permitAll()
                    .pathMatchers("/login/**").permitAll()
                    .pathMatchers("/error/**").permitAll()
                    .pathMatchers("/openapi/**").permitAll()
                    .pathMatchers("/webjars/**").permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2ResourceServer ->
                oauth2ResourceServer
                    .jwt(Customizer.withDefaults())
            }
        return http.build()
    }

}