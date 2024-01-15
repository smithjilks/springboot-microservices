package com.smithjilks.springcloud.gateway.config


import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient


@Configuration
class ApplicationConfig {

    @Bean
    @LoadBalanced
    fun loadBalancedWebClientBuilder(): WebClient.Builder = WebClient.builder()
}