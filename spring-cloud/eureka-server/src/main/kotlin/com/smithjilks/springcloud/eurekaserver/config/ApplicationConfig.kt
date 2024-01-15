package com.smithjilks.springcloud.eurekaserver.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain


@Configuration
class ApplicationConfig(
    @Value("\${app.eureka-username}") val username: String,
    @Value("\${app.eureka-password}") val password: String
) {


    @Bean
    fun userDetailsService(): InMemoryUserDetailsManager {
        val user: UserDetails = User.withDefaultPasswordEncoder()
            .username(username)
            .password(password)
            .roles("USER")
            .build()
        return InMemoryUserDetailsManager(user)
    }

    @Bean
    @Throws(Exception::class)
    fun configure(http: HttpSecurity): SecurityFilterChain {
        http
            // Disable csrf to allow services to register themselves with Eureka
            .csrf { csrf -> csrf.disable() }
            .authorizeHttpRequests { requests ->
                requests.anyRequest().authenticated()
            }
            .httpBasic(Customizer.withDefaults())
        return http.build()
    }

}