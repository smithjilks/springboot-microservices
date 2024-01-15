package com.smithjilks.microservices.core.recommendation

import com.smithjilks.microservices.api.core.recommendation.Recommendation
import com.smithjilks.microservices.api.event.Event
import com.smithjilks.microservices.api.exceptions.InvalidInputException
import com.smithjilks.microservices.core.recommendation.persistence.RecommendationRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec
import reactor.core.publisher.Mono.just
import java.util.function.Consumer


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["eureka.client.enabled=false"]
)
@AutoConfigureWebTestClient(timeout = "36000")
class RecommendationServiceApplicationTests : MongoDbTestBase() {
    @Autowired
    lateinit var client: WebTestClient

    @Autowired
    lateinit var repository: RecommendationRepository

    @Autowired
    @Qualifier("messageProcessor")
    lateinit var messageProcessor: Consumer<Event<Int, Recommendation?>>

    @BeforeEach
    fun setupDb() {
        repository.deleteAll().block()
    }

    @Test
    fun getRecommendationsByProductId() {
        val productId = 1
//        postAndVerifyRecommendation(productId, 1, HttpStatus.OK)
//        postAndVerifyRecommendation(productId, 2, HttpStatus.OK)
//        postAndVerifyRecommendation(productId, 3, HttpStatus.OK)
//        Assertions.assertEquals(3, repository.findByProductId(productId).size)
//        getAndVerifyRecommendationsByProductId(productId, HttpStatus.OK)
//            .jsonPath("$.length()").isEqualTo(3)
//            .jsonPath("$[2].productId").isEqualTo(productId)
//            .jsonPath("$[2].recommendationId").isEqualTo(3)

        sendCreateRecommendationEvent(productId, 1)
        sendCreateRecommendationEvent(productId, 2)
        sendCreateRecommendationEvent(productId, 3)

        Assertions.assertEquals(3, repository.findByProductId(productId).count().block() as Long)

        getAndVerifyRecommendationsByProductId(productId, HttpStatus.OK)
            .jsonPath("$.length()").isEqualTo(3)
            .jsonPath("$[2].productId").isEqualTo(productId)
            .jsonPath("$[2].recommendationId").isEqualTo(3)
    }

    @Test
    fun duplicateError() {
        val productId = 1
        val recommendationId = 1
//        postAndVerifyRecommendation(productId, recommendationId, HttpStatus.OK)
//            .jsonPath("$.productId").isEqualTo(productId)
//            .jsonPath("$.recommendationId").isEqualTo(recommendationId)
//        Assertions.assertEquals(1, repository.count())
//
//        postAndVerifyRecommendation(productId, recommendationId, HttpStatus.UNPROCESSABLE_ENTITY)
//            .jsonPath("$.path").isEqualTo("/recommendation")
//            .jsonPath("$.message").isEqualTo("Duplicate key, Product Id:  1, Recommendation Id: 1")
//        Assertions.assertEquals(1, repository.count())

        sendCreateRecommendationEvent(productId, recommendationId)

        Assertions.assertEquals(1, repository.count().block() as Long)

        val thrown = assertThrows(
            InvalidInputException::class.java,
            { sendCreateRecommendationEvent(productId, recommendationId) },
            "Expected a InvalidInputException here!"
        )
        Assertions.assertEquals("Duplicate key, Product Id: 1, Recommendation Id: 1", thrown.message)

        Assertions.assertEquals(1, repository.count().block() as Long)
    }

    @Test
    fun deleteRecommendations() {
        val productId = 1
        val recommendationId = 1
//        postAndVerifyRecommendation(productId, recommendationId, HttpStatus.OK)
//        Assertions.assertEquals(1, repository.findByProductId(productId).size)
//        deleteAndVerifyRecommendationsByProductId(productId, HttpStatus.OK)
//        Assertions.assertEquals(0, repository.findByProductId(productId).size)
//        deleteAndVerifyRecommendationsByProductId(productId, HttpStatus.OK)

        sendCreateRecommendationEvent(productId, recommendationId)
        Assertions.assertEquals(1L, repository.findByProductId(productId).count().block())

        sendDeleteRecommendationEvent(productId)
        Assertions.assertEquals(0L, repository.findByProductId(productId).count().block())

        sendDeleteRecommendationEvent(productId)
    }

    @Test
    fun getRecommendationsMissingParameter() {
        getAndVerifyRecommendationsByProductId("", HttpStatus.BAD_REQUEST)
            .jsonPath("$.path").isEqualTo("/recommendation")
            .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.")
    }

    @Test
    fun getRecommendationsInvalidParameter() {
        getAndVerifyRecommendationsByProductId("?productId=no-integer", HttpStatus.BAD_REQUEST)
            .jsonPath("$.path").isEqualTo("/recommendation")
            .jsonPath("$.message").isEqualTo("Type mismatch.")
    }

    @Test
    fun getRecommendationsNotFound() {
        getAndVerifyRecommendationsByProductId("?productId=113", HttpStatus.OK)
            .jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    fun getRecommendationsInvalidParameterNegativeValue() {
        val productIdInvalid = -1
        getAndVerifyRecommendationsByProductId("?productId=$productIdInvalid", HttpStatus.UNPROCESSABLE_ENTITY)
            .jsonPath("$.path").isEqualTo("/recommendation")
            .jsonPath("$.message").isEqualTo("Invalid productId: $productIdInvalid")
    }

    private fun getAndVerifyRecommendationsByProductId(productId: Int, expectedStatus: HttpStatus): BodyContentSpec {
        return getAndVerifyRecommendationsByProductId("?productId=$productId", expectedStatus)
    }

    private fun getAndVerifyRecommendationsByProductId(
        productIdQuery: String,
        expectedStatus: HttpStatus
    ): BodyContentSpec {
        return client.get()
            .uri("/recommendation$productIdQuery")
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectHeader().contentType(APPLICATION_JSON)
            .expectBody()
    }

    private fun postAndVerifyRecommendation(
        productId: Int,
        recommendationId: Int,
        expectedStatus: HttpStatus
    ): BodyContentSpec {
        val recommendation = Recommendation(
            productId, recommendationId,
            "Author $recommendationId", recommendationId, "Content $recommendationId", "SA"
        )
        return client.post()
            .uri("/recommendation")
            .body(just(recommendation), Recommendation::class.java)
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectHeader().contentType(APPLICATION_JSON)
            .expectBody()
    }

    private fun deleteAndVerifyRecommendationsByProductId(productId: Int, expectedStatus: HttpStatus): BodyContentSpec {
        return client.delete()
            .uri("/recommendation?productId=$productId")
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectBody()
    }

    private fun sendCreateRecommendationEvent(productId: Int, recommendationId: Int) {
        val recommendation = Recommendation(
            productId, recommendationId,
            "Author $recommendationId", recommendationId, "Content $recommendationId", "SA"
        )
        val event: Event<Int, Recommendation?> = Event(Event.Type.CREATE, productId, recommendation)
        messageProcessor.accept(event)
    }

    private fun sendDeleteRecommendationEvent(productId: Int) {
        val event: Event<Int, Recommendation?> = Event(Event.Type.DELETE, productId, null)
        messageProcessor.accept(event)
    }
}
