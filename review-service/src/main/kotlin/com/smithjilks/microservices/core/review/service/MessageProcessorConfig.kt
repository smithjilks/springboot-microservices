package com.smithjilks.microservices.core.recommendation.service

import com.smithjilks.microservices.api.core.review.Review
import com.smithjilks.microservices.api.core.review.ReviewService
import com.smithjilks.microservices.api.event.Event
import com.smithjilks.microservices.api.exceptions.EventProcessingException
import mu.KLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.function.Consumer


@Configuration
class MessageProcessorConfig(
    val reviewService: ReviewService
) {
    companion object : KLogging()

    @Bean
    fun messageProcessor(): Consumer<Event<Int, Review>> {
        return Consumer<Event<Int, Review>> { event ->
            logger.info { "Process message created at ${event.eventCreatedAt}" }

            when (event.eventType) {
                Event.Type.CREATE -> {
                    val review: Review = event.data
                    logger.info { "Create recommendation with ID: ${review.productId}/${review.reviewId}" }
                    reviewService.createReview(review)!!.block()
                }

                Event.Type.DELETE -> {
                    val productId = event.key
                    logger.info { "Delete recommendation with ProductID: $productId" }
                    reviewService.deleteReviews(productId)!!.block()
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