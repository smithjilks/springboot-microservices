package com.smithjilks.springcloud.gateway.config

import mu.KLogging
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthContributor
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.logging.Level


@Configuration
class HealthCheckConfiguration(
    val webClient: WebClient.Builder
) {

    companion object : KLogging()

    @Bean
    fun healthCheckMicroservices(): ReactiveHealthContributor {
        val registry = HashMap<String, ReactiveHealthIndicator>()

        registry.put("product") { getHealth("http://product") }
        registry.put("recommendation") { getHealth("http://recommendation") }
        registry.put("review") { getHealth("http://review") }
        registry.put("product-composite") { getHealth("http://product-composite") }

        return CompositeReactiveHealthContributor.fromMap(registry)
    }

    private fun getHealth(baseUrl: String): Mono<Health> {
        val url = "$baseUrl/actuator/health"
        logger.debug { "Setting up a cll to the Health API on URL: $url" }
        return webClient.build().get().uri(url).retrieve().bodyToMono(String::class.java)
            .map { _ -> Health.Builder().up().build() }
            .onErrorResume { ex -> Mono.just(Health.Builder().down(ex).build()) }
            .log(logger.name, Level.FINE)
    }
}