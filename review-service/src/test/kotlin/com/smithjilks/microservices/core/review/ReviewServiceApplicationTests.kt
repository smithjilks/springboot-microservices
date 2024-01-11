package com.smithjilks.microservices.core.review

import com.smithjilks.microservices.api.core.review.Review
import com.smithjilks.microservices.api.event.Event
import com.smithjilks.microservices.api.exceptions.InvalidInputException
import com.smithjilks.microservices.core.review.persistence.ReviewRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec
import reactor.core.publisher.Mono.just
import java.util.function.Consumer


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.cloud.stream.defaultBinder=rabbit", "com.logging.level.smithjilks.microservices=DEBUG"]
)
@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "36000")
internal class ReviewServiceApplicationTests : PostgreSqlTestBase() {
    @Autowired
    lateinit var client: WebTestClient

    @Autowired
    lateinit var repository: ReviewRepository

    @Autowired
    @Qualifier("messageProcessor")
    lateinit var messageProcessor: Consumer<Event<Int, Review?>>

    @BeforeEach
    fun setupDb() {
        repository.deleteAll()
    }

    @Test
    fun getReviewsByProductId() {
        val productId = 1
        Assertions.assertEquals(0, repository.findByProductId(productId).size)

//        postAndVerifyReview(productId, 1, HttpStatus.OK)
//        postAndVerifyReview(productId, 2, HttpStatus.OK)
//        postAndVerifyReview(productId, 3, HttpStatus.OK)

        sendCreateReviewEvent(productId, 1)
        sendCreateReviewEvent(productId, 2)
        sendCreateReviewEvent(productId, 3)


        Assertions.assertEquals(3, repository.findByProductId(productId).size)

        getAndVerifyReviewsByProductId(productId, HttpStatus.OK)
            .jsonPath("$.length()").isEqualTo(3)
            .jsonPath("$[2].productId").isEqualTo(productId)
            .jsonPath("$[2].reviewId").isEqualTo(3)
    }

    @Test
    fun duplicateError() {
        val productId = 1
        val reviewId = 1
        Assertions.assertEquals(0, repository.count())
//        postAndVerifyReview(productId, reviewId, HttpStatus.OK)
//            .jsonPath("$.productId").isEqualTo(productId)
//            .jsonPath("$.reviewId").isEqualTo(reviewId)

        sendCreateReviewEvent(productId, reviewId)

        Assertions.assertEquals(1, repository.count())
//        postAndVerifyReview(productId, reviewId, HttpStatus.UNPROCESSABLE_ENTITY)
//            .jsonPath("$.path").isEqualTo("/review")
//            .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 1 Review Id: 1")

        val thrown: InvalidInputException = Assertions.assertThrows(
            InvalidInputException::class.java,
            { sendCreateReviewEvent(productId, reviewId) },
            "Expected a InvalidInputException here!"
        )
        Assertions.assertEquals("Duplicate key, Product Id: 1, Review Id: 1", thrown.message)

        Assertions.assertEquals(1, repository.count())
    }

    @Test
    fun deleteReviews() {
        val productId = 1
        val reviewId = 1
//        postAndVerifyReview(productId, reviewId, HttpStatus.OK)
//        Assertions.assertEquals(1, repository.findByProductId(productId).size)
//        deleteAndVerifyReviewsByProductId(productId, HttpStatus.OK)
//        Assertions.assertEquals(0, repository.findByProductId(productId).size)
//        deleteAndVerifyReviewsByProductId(productId, HttpStatus.OK)

        sendCreateReviewEvent(productId, reviewId)
        Assertions.assertEquals(1, repository.findByProductId(productId).size)

        sendDeleteReviewEvent(productId)
        Assertions.assertEquals(0, repository.findByProductId(productId).size)

        sendDeleteReviewEvent(productId)
    }

    @Test
    fun getReviewsMissingParameter() {
        getAndVerifyReviewsByProductId("", HttpStatus.BAD_REQUEST)
            .jsonPath("$.path").isEqualTo("/review")
            .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.")
    }

    @Test
    fun getReviewsInvalidParameter() {
        getAndVerifyReviewsByProductId("?productId=no-integer", HttpStatus.BAD_REQUEST)
            .jsonPath("$.path").isEqualTo("/review")
            .jsonPath("$.message").isEqualTo("Type mismatch.")
    }

    @Test
    fun getReviewsNotFound() {
        getAndVerifyReviewsByProductId("?productId=213", HttpStatus.OK)
            .jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    fun getReviewsInvalidParameterNegativeValue() {
        val productIdInvalid = -1
        getAndVerifyReviewsByProductId("?productId=$productIdInvalid", HttpStatus.UNPROCESSABLE_ENTITY)
            .jsonPath("$.path").isEqualTo("/review")
            .jsonPath("$.message").isEqualTo("Invalid productId: $productIdInvalid")
    }

    private fun getAndVerifyReviewsByProductId(productId: Int, expectedStatus: HttpStatus): BodyContentSpec {
        return getAndVerifyReviewsByProductId("?productId=$productId", expectedStatus)
    }

    private fun getAndVerifyReviewsByProductId(productIdQuery: String, expectedStatus: HttpStatus): BodyContentSpec {
        return client.get()
            .uri("/review$productIdQuery")
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectHeader().contentType(APPLICATION_JSON)
            .expectBody()
    }

    private fun postAndVerifyReview(productId: Int, reviewId: Int, expectedStatus: HttpStatus): BodyContentSpec {
        val review = Review(
            productId, reviewId,
            "Author $reviewId", "Subject $reviewId", "Content $reviewId", "SA"
        )
        return client.post()
            .uri("/review")
            .body(just(review), Review::class.java)
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectHeader().contentType(APPLICATION_JSON)
            .expectBody()
    }

    private fun deleteAndVerifyReviewsByProductId(productId: Int, expectedStatus: HttpStatus): BodyContentSpec {
        return client.delete()
            .uri("/review?productId=$productId")
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectBody()
    }

    private fun sendCreateReviewEvent(productId: Int, reviewId: Int) {
        val review: Review? = Review(
            productId, reviewId,
            "Author $reviewId", "Subject $reviewId", "Content $reviewId", "SA"
        )
        val event = Event(Event.Type.CREATE, productId, review)
        messageProcessor.accept(event)
    }

    private fun sendDeleteReviewEvent(productId: Int) {
        val event: Event<Int, Review?> = Event(Event.Type.DELETE, productId, null)
        messageProcessor.accept(event)
    }
}
