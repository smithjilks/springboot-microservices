package com.smithjilks.microservices.composite.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.smithjilks.microservices.api.core.product.Product
import com.smithjilks.microservices.api.core.product.ProductService
import com.smithjilks.microservices.api.core.recommendation.Recommendation
import com.smithjilks.microservices.api.core.recommendation.RecommendationService
import com.smithjilks.microservices.api.core.review.Review
import com.smithjilks.microservices.api.core.review.ReviewService
import com.smithjilks.microservices.api.event.Event
import com.smithjilks.microservices.api.exceptions.InvalidInputException
import com.smithjilks.microservices.api.exceptions.NotFoundException
import com.smithjilks.microservices.util.HttpErrorInfo
import mu.KLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.Health
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.http.HttpStatus
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Flux.empty
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import java.io.IOException
import java.util.logging.Level.FINE


@Component
class ProductCompositeIntegration(
    val restTemplate: RestTemplate,
    val objectMapper: ObjectMapper,
    val streamBridge: StreamBridge,
    var webClient: WebClient.Builder,
    @Qualifier("publishEventScheduler") val publishEventScheduler: Scheduler,
//    @Value("\${app.product-service.host}") val productServiceHost: String,
//    @Value("\${app.product-service.port}") val productServicePort: Int,
//    @Value("\${app.recommendation-service.host}") val recommendationServiceHost: String,
//    @Value("\${app.recommendation-service.port}") val recommendationServicePort: Int,
//    @Value("\${app.review-service.host}") val reviewServiceHost: String,
//    @Value("\${app.review-service.port}") val reviewServicePort: Int
) : ProductService, RecommendationService, ReviewService {
    companion object : KLogging()

    private val productServiceUrl = "http://product"
    private val recommendationServiceUrl = "http://recommendation"
    private val reviewServiceUrl = "http://review"

//    init {
//        productServiceUrl = "http://$productServiceHost:$productServicePort/product"
//        recommendationServiceUrl =
//            "http://$recommendationServiceHost:$recommendationServicePort/recommendation"
//        reviewServiceUrl = "http://$reviewServiceHost:$reviewServicePort/review"
//    }

    override fun getProduct(productId: Int): Mono<Product>? {
//        try {
//            val url = "$productServiceUrl/$productId"
//            logger.debug { "Will call getProduct API on URL:$url" }
//
//            val product = restTemplate.getForObject(url, Product::class.java)
//            logger.debug { "Found a product with id: ${product?.productId}" }
//
//            return product
//
//        } catch (ex: HttpClientErrorException) {
//            throw handleHttpClientException(ex)
//        }

        val url = "$productServiceUrl/product/$productId"
        logger.debug { "Will call getProduct API on URL:$url" }

        return webClient.build().get().uri(url).retrieve().bodyToMono(Product::class.java).log(logger.name, FINE)
            .onErrorMap(
                WebClientResponseException::class.java
            ) { ex -> handleException(ex) }

    }

    override fun createProduct(body: Product): Mono<Product>? {
//        return try {
//            val url = productServiceUrl
//            logger.debug { "Will post a new product to URL: $url" }
//            val product = restTemplate.postForObject(
//                url, body,
//                Product::class.java
//            )
//            logger.debug { "Created a product with id: ${product?.productId}" }
//            product
//        } catch (ex: HttpClientErrorException) {
//            throw handleHttpClientException(ex)
//        }

        return Mono.fromCallable {
            sendMessage("products-out-0", Event(Event.Type.CREATE, body.productId, body))
            body
        }.subscribeOn(publishEventScheduler)
    }

    override fun deleteProduct(productId: Int): Mono<Void>? {
//        try {
//            val url = "$productServiceUrl/$productId"
//            logger.debug { "Will call the deleteProduct API on URL: $url" }
//            restTemplate.delete(url)
//        } catch (ex: HttpClientErrorException) {
//            throw handleHttpClientException(ex)
//        }

        return Mono.fromRunnable<Void?> {
            sendMessage("products-out-0", Event(Event.Type.DELETE, productId, null))
        }.subscribeOn(publishEventScheduler).then()
    }

    override fun getRecommendations(productId: Int): Flux<Recommendation> {
//        return try {
//            val url = "$recommendationServiceUrl$productId"
//            logger.debug { "Will call getRecommendations API on URL: $url" }
//
//            val recommendations = restTemplate
//                .exchange(url, HttpMethod.GET, null, object : ParameterizedTypeReference<List<Recommendation>>() {})
//                .body
//            logger.debug { "Found ${recommendations?.size} recommendations for a product with id: $productId" }
//
//            recommendations ?: emptyList()
//        } catch (ex: HttpClientErrorException) {
//            logger.warn { "Got an exception while requesting recommendations, return zero recommendations: ${ex.message}" }
//            emptyList()
//        }

        val url = "$recommendationServiceUrl/recommendation?productId=$productId"
        logger.debug { "Will call getRecommendations API on URL: $url" }

        // Return an empty result if something goes wrong to make it possible for the composite service to return partial responses
        return webClient.build().get().uri(url).retrieve().bodyToFlux(Recommendation::class.java)
            .log(logger.name, FINE).onErrorResume { empty() }

    }

    override fun createRecommendation(body: Recommendation): Mono<Recommendation>? {
//        return try {
//            val url = recommendationServiceUrl
//            logger.debug { "Will post a new recommendation to URL: $url" }
//            val recommendation = restTemplate.postForObject(url, body, Recommendation::class.java)
//            logger.debug { "Created a recommendation with id: ${recommendation?.productId}" }
//            recommendation
//        } catch (ex: HttpClientErrorException) {
//            throw handleHttpClientException(ex)
//        }

        return Mono.fromCallable {
            sendMessage("recommendations-out-0", Event(Event.Type.CREATE, body.productId, body))
            body
        }.subscribeOn(publishEventScheduler)
    }

    override fun deleteRecommendations(productId: Int): Mono<Void> {
//        try {
//            val url = "$recommendationServiceUrl$productId"
//            logger.debug { "Will call the deleteRecommendations API on URL: $url" }
//            restTemplate.delete(url)
//        } catch (ex: HttpClientErrorException) {
//            throw handleHttpClientException(ex)
//        }

        return Mono.fromRunnable<Any> {
            sendMessage("recommendations-out-0", Event(Event.Type.DELETE, productId, null))
        }.subscribeOn(publishEventScheduler).then()
    }

    override fun getReviews(productId: Int): Flux<Review> {
//        return try {
//            val url = "$reviewServiceUrl$productId"
//            logger.debug { "Will call getReviews API on URL: $url" }
//
//            val reviews = restTemplate
//                .exchange(url, HttpMethod.GET, null, object : ParameterizedTypeReference<List<Review>>() {})
//                .body
//            logger.debug { "Found ${reviews?.size} reviews for a product with id: $productId" }
//
//            reviews ?: emptyList()
//        } catch (ex: HttpClientErrorException) {
//            logger.warn { "Got an exception while requesting reviews. Return zero reviews: ${ex.message}" }
//            emptyList()
//        }

        val url = "$reviewServiceUrl/review?productId=$productId"

        logger.debug { "Will call the getReviews API on URL: $url" }

        // Return an empty result if something goes wrong to make it possible for the composite service to return partial responses
        return webClient.build().get().uri(url).retrieve().bodyToFlux(Review::class.java).log(logger.name, FINE)
            .onErrorResume { empty() }

    }

    override fun createReview(body: Review): Mono<Review>? {
//        return try {
//            val url = reviewServiceUrl
//            logger.debug { "Will post a new review to URL: $url" }
//            val review = restTemplate.postForObject(url, body, Review::class.java)
//            logger.debug { "Created a review with id: ${review?.productId}" }
//            review
//        } catch (ex: HttpClientErrorException) {
//            throw handleHttpClientException(ex)
//        }

        return Mono.fromCallable {
            sendMessage("reviews-out-0", Event(Event.Type.CREATE, body.productId, body))
            body
        }.subscribeOn(publishEventScheduler)
    }

    override fun deleteReviews(productId: Int): Mono<Void>? {
//        try {
//            val url = "$reviewServiceUrl$productId"
//            logger.debug("Will call the deleteReviews API on URL: $url")
//            restTemplate.delete(url)
//        } catch (ex: HttpClientErrorException) {
//            throw handleHttpClientException(ex)
//        }
        return Mono.fromRunnable<Void?> {
            sendMessage("reviews-out-0", Event(Event.Type.DELETE, productId, null))
        }.subscribeOn(publishEventScheduler).then()
    }

    fun getProductHealth(): Mono<Health> {
        return getHealth(productServiceUrl)
    }

    fun getRecommendationHealth(): Mono<Health> {
        return getHealth(recommendationServiceUrl)
    }

    fun getReviewHealth(): Mono<Health> {
        return getHealth(reviewServiceUrl)
    }

    private fun getHealth(url: String): Mono<Health> {
        val endPoint = "$url/actuator/health"
        logger.debug { "Will call the Health API on URL: $endPoint" }
        return webClient.build().get().uri(endPoint).retrieve().bodyToMono(String::class.java)
            .map { Health.Builder().up().build() }
            .onErrorResume { ex: Throwable? ->
                Mono.just(
                    Health.Builder().down(ex).build()
                )
            }
            .log(logger.name, FINE)
    }

    private fun sendMessage(bindingName: String, event: Event<*, *>) {
        logger.debug { "Sending a ${event.eventType} message to $bindingName" }
        val message = MessageBuilder.withPayload(event)
            .setHeader("partitionKey", event.key)
            .build()
        streamBridge.send(bindingName, message)
    }

    private fun getErrorMessage(ex: HttpClientErrorException): String? {
        return try {
            objectMapper.readValue(ex.responseBodyAsString, HttpErrorInfo::class.java).message
        } catch (ioException: IOException) {
            ex.message
        }
    }

    private fun handleHttpClientException(ex: HttpClientErrorException): RuntimeException {
        return when (HttpStatus.resolve(ex.statusCode.value())) {
            HttpStatus.NOT_FOUND -> NotFoundException(
                getErrorMessage(ex)!!
            )

            HttpStatus.UNPROCESSABLE_ENTITY -> InvalidInputException(getErrorMessage(ex)!!)
            else -> {
                logger.warn { "Got an unexpected HTTP error: ${ex.statusCode}, will rethrow it" }
                logger.warn { "Error body: ${ex.responseBodyAsString}" }
                ex
            }
        }
    }

    private fun handleException(ex: Throwable): Throwable {
        if (ex !is WebClientResponseException) {
            logger.warn("Got a unexpected error: {}, will rethrow it", ex.toString())
            return ex
        }
        val wcre = ex
        return when (HttpStatus.resolve(wcre.statusCode.value())) {
            HttpStatus.NOT_FOUND -> NotFoundException(
                getErrorMessage(wcre)!!
            )

            HttpStatus.UNPROCESSABLE_ENTITY -> InvalidInputException(getErrorMessage(wcre)!!)
            else -> {
                logger.warn { "Got an unexpected HTTP error: ${wcre.statusCode}, will rethrow it" }
                logger.warn { "Error body: ${wcre.responseBodyAsString}" }
                ex
            }
        }
    }

    private fun getErrorMessage(ex: WebClientResponseException): String? {
        return try {
            objectMapper.readValue(ex.responseBodyAsString, HttpErrorInfo::class.java).message
        } catch (ioex: IOException) {
            ex.message
        }
    }
}