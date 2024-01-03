package com.smithjilks.microservices.composite.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.smithjilks.microservices.api.core.product.Product
import com.smithjilks.microservices.api.core.product.ProductService
import com.smithjilks.microservices.api.core.recommendation.Recommendation
import com.smithjilks.microservices.api.core.recommendation.RecommendationService
import com.smithjilks.microservices.api.core.review.Review
import com.smithjilks.microservices.api.core.review.ReviewService
import com.smithjilks.microservices.api.exceptions.InvalidInputException
import com.smithjilks.microservices.api.exceptions.NotFoundException
import com.smithjilks.microservices.util.HttpErrorInfo
import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.io.IOException


@Component
class ProductCompositeIntegration(
    val restTemplate: RestTemplate,
    val objectMapper: ObjectMapper,
    @Value("\${app.product-service.host}") val productServiceHost: String,
    @Value("\${app.product-service.port}") val productServicePort: Int,
    @Value("\${app.recommendation-service.host}") val recommendationServiceHost: String,
    @Value("\${app.recommendation-service.port}") val recommendationServicePort: Int,
    @Value("\${app.review-service.host}") val reviewServiceHost: String,
    @Value("\${app.review-service.port}") val reviewServicePort: Int
) : ProductService, RecommendationService, ReviewService {
    companion object : KLogging()

    private final val productServiceUrl: String
    private val recommendationServiceUrl: String
    private val reviewServiceUrl: String

    init {
        productServiceUrl = "http://$productServiceHost:$productServicePort/product/"
        recommendationServiceUrl =
            "http://$recommendationServiceHost:$recommendationServicePort/recommendation?productId="
        reviewServiceUrl = "http://$reviewServiceHost:$reviewServicePort/review?productId="
    }


    override fun getProduct(productId: Int): Product? {
        try {
            val url = productServiceUrl + productId
            logger.debug { "Will call getProduct API on URL:$url" }

            val product = restTemplate.getForObject(url, Product::class.java)
            logger.debug { "Found a product with id: ${product?.productId}" }

            return product

        } catch (ex: HttpClientErrorException) {
            throw handleHttpClientException(ex)
        }
    }

    override fun createProduct(body: Product): Product? {
        return try {
            val url = productServiceUrl
            logger.debug { "Will post a new product to URL: $url" }
            val product = restTemplate.postForObject(
                url, body,
                Product::class.java
            )
            logger.debug { "Created a product with id: ${product?.productId}" }
            product
        } catch (ex: HttpClientErrorException) {
            throw handleHttpClientException(ex)
        }
    }

    override fun deleteProduct(productId: Int) {
        try {
            val url = "$productServiceUrl/$productId"
            logger.debug { "Will call the deleteProduct API on URL: $url" }
            restTemplate.delete(url)
        } catch (ex: HttpClientErrorException) {
            throw handleHttpClientException(ex)
        }
    }

    override fun getRecommendations(productId: Int): List<Recommendation> {
        return try {
            val url = recommendationServiceUrl + productId
            logger.debug { "Will call getRecommendations API on URL: $url" }

            val recommendations = restTemplate
                .exchange(url, HttpMethod.GET, null, object : ParameterizedTypeReference<List<Recommendation>>() {})
                .body
            logger.debug { "Found ${recommendations?.size} recommendations for a product with id: $productId" }

            recommendations ?: emptyList()
        } catch (ex: HttpClientErrorException) {
            logger.warn { "Got an exception while requesting recommendations, return zero recommendations: ${ex.message}" }
            emptyList()
        }
    }

    override fun createRecommendation(body: Recommendation): Recommendation? {
        return try {
            val url = recommendationServiceUrl
            logger.debug { "Will post a new recommendation to URL: $url" }
            val recommendation = restTemplate.postForObject(url, body, Recommendation::class.java)
            logger.debug { "Created a recommendation with id: ${recommendation?.productId}" }
            recommendation
        } catch (ex: HttpClientErrorException) {
            throw handleHttpClientException(ex)
        }
    }

    override fun deleteRecommendations(productId: Int) {
        try {
            val url = "$recommendationServiceUrl?productId=$productId"
            logger.debug { "Will call the deleteRecommendations API on URL: $url" }
            restTemplate.delete(url)
        } catch (ex: HttpClientErrorException) {
            throw handleHttpClientException(ex)
        }
    }

    override fun getReviews(productId: Int): List<Review> {
        return try {
            val url = reviewServiceUrl + productId
            logger.debug { "Will call getReviews API on URL: $url" }

            val reviews = restTemplate
                .exchange(url, HttpMethod.GET, null, object : ParameterizedTypeReference<List<Review>>() {})
                .body
            logger.debug { "Found ${reviews?.size} reviews for a product with id: $productId" }

            reviews ?: emptyList()
        } catch (ex: HttpClientErrorException) {
            logger.warn { "Got an exception while requesting reviews. Return zero reviews: ${ex.message}" }
            emptyList()
        }
    }

    override fun createReview(body: Review): Review? {
        return try {
            val url = reviewServiceUrl
            logger.debug { "Will post a new review to URL: $url" }
            val review = restTemplate.postForObject(url, body, Review::class.java)
            logger.debug { "Created a review with id: ${review?.productId}" }
            review
        } catch (ex: HttpClientErrorException) {
            throw handleHttpClientException(ex)
        }
    }

    override fun deleteReviews(productId: Int) {
        try {
            val url = "$reviewServiceUrl?productId=$productId"
            logger.debug("Will call the deleteReviews API on URL: $url")
            restTemplate.delete(url)
        } catch (ex: HttpClientErrorException) {
            throw handleHttpClientException(ex)
        }
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
}