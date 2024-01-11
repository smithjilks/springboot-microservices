package com.smithjilks.microservices.core.recommendation.service

import com.smithjilks.microservices.api.core.recommendation.Recommendation
import com.smithjilks.microservices.api.core.recommendation.RecommendationService
import com.smithjilks.microservices.api.event.Event
import com.smithjilks.microservices.api.exceptions.EventProcessingException
import mu.KLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.function.Consumer


@Configuration
class MessageProcessorConfig(
    val recommendationService: RecommendationService
) {

    companion object : KLogging()

    @Bean
    fun messageProcessor(): Consumer<Event<Int, Recommendation>> {
        return Consumer<Event<Int, Recommendation>> { event ->
            logger.info { "Process message created at ${event.eventCreatedAt}" }

            when (event.eventType) {
                Event.Type.CREATE -> {
                    val recommendation: Recommendation = event.data
                    logger.info { "Create recommendation with ID: ${recommendation.productId}/${recommendation.recommendationId}" }
                    recommendationService.createRecommendation(recommendation)!!.block()
                }

                Event.Type.DELETE -> {
                    val productId = event.key
                    logger.info { "Delete recommendation with ProductID: $productId" }
                    recommendationService.deleteRecommendations(productId)!!.block()
                }

                else -> {
                    val errorMessage = "Incorrect event type: ${event.eventType}, expected a CREATE or DELETE event"
                    logger.warn(errorMessage)
                    throw EventProcessingException(errorMessage)
                }
            }

            logger.info("Message processing done!")
        }
    }
}