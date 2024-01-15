package com.smithjilks.microservices.composite.product

import com.smithjilks.microservices.api.composite.product.ProductAggregate
import com.smithjilks.microservices.api.composite.product.RecommendationSummary
import com.smithjilks.microservices.api.composite.product.ReviewSummary
import com.smithjilks.microservices.api.composite.product.ServiceAddresses
import com.smithjilks.microservices.api.core.product.Product
import com.smithjilks.microservices.api.core.recommendation.Recommendation
import com.smithjilks.microservices.api.core.review.Review
import com.smithjilks.microservices.api.event.Event
import com.smithjilks.microservices.composite.product.IsSameEvent.Companion.sameEventExceptCreatedAt
import mu.KLogging
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.binder.test.OutputDestination
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.messaging.Message
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono.just


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.main.allow-bean-definition-overriding=true", "eureka.client.enabled=false"]
)
@Import(
    TestChannelBinderConfiguration::class
)
class MessagingTests {
    @Autowired
    lateinit var client: WebTestClient

    @Autowired
    lateinit var target: OutputDestination

    @BeforeEach
    fun setUp() {
        purgeMessages("products")
        purgeMessages("recommendations")
        purgeMessages("reviews")
    }

    @Test
    fun createCompositeProduct1() {
        val composite = ProductAggregate(1, "name", 1f, listOf(), listOf(), ServiceAddresses("", "", "", ""))
        postAndVerifyProduct(composite, HttpStatus.ACCEPTED)
        val productMessages = getMessages("products")
        val recommendationMessages = getMessages("recommendations")
        val reviewMessages = getMessages("reviews")

        // Assert one expected new product event queued up
        Assertions.assertEquals(1, productMessages.size)
        val expectedEvent: Event<Int, Product> =
            Event(
                Event.Type.CREATE,
                composite.productId,
                Product(composite.productId, composite.name, composite.weight, null)
            )
        assertThat(productMessages[0], `is`(sameEventExceptCreatedAt(expectedEvent)))

        // Assert no recommendation and review events
        Assertions.assertEquals(0, recommendationMessages.size)
        Assertions.assertEquals(0, reviewMessages.size)
    }

    @Test
    fun createCompositeProduct2() {
        val composite = ProductAggregate(
            1, "name", 1f,
            listOf(RecommendationSummary(1, "a", 1, "c")),
            listOf(ReviewSummary(1, "a", "s", "c")), ServiceAddresses("", "", "", "")
        )
        postAndVerifyProduct(composite, HttpStatus.ACCEPTED)
        val productMessages = getMessages("products")
        val recommendationMessages = getMessages("recommendations")
        val reviewMessages = getMessages("reviews")

        // Assert one create product event queued up
        Assertions.assertEquals(1, productMessages.size)
        val expectedProductEvent: Event<Int, Product> =
            Event(
                Event.Type.CREATE,
                composite.productId,
                Product(composite.productId, composite.name, composite.weight, null)
            )
        assertThat(productMessages[0], `is`(sameEventExceptCreatedAt(expectedProductEvent)))

        // Assert one create recommendation event queued up
        Assertions.assertEquals(1, recommendationMessages.size)
        val (recommendationId, author, rate, content) = composite.recommendations[0]
        val expectedRecommendationEvent: Event<Int, Recommendation> = Event(
            Event.Type.CREATE, composite.productId,
            Recommendation(composite.productId, recommendationId, author, rate, content, null)
        )
        assertThat(recommendationMessages[0], `is`(sameEventExceptCreatedAt(expectedRecommendationEvent)))

        // Assert one create review event queued up
        Assertions.assertEquals(1, reviewMessages.size)
        val (reviewId, author1, subject, content1) = composite.reviews[0]
        val expectedReviewEvent: Event<Int, Review> = Event(
            Event.Type.CREATE, composite.productId, Review(
                composite.productId,
                reviewId, author1, subject, content1, null
            )
        )
        assertThat(reviewMessages[0], `is`(sameEventExceptCreatedAt(expectedReviewEvent)))
    }

    @Test
    fun deleteCompositeProduct() {
        deleteAndVerifyProduct(1, HttpStatus.ACCEPTED)
        val productMessages = getMessages("products")
        val recommendationMessages = getMessages("recommendations")
        val reviewMessages = getMessages("reviews")

        // Assert one delete product event queued up
        Assertions.assertEquals(1, productMessages.size)
        val expectedProductEvent: Event<Int, Product?> = Event(Event.Type.DELETE, 1, null)
        assertThat(productMessages[0], `is`(sameEventExceptCreatedAt(expectedProductEvent)))

        // Assert one delete recommendation event queued up
        Assertions.assertEquals(1, recommendationMessages.size)
        val expectedRecommendationEvent: Event<Int, Recommendation?> = Event(Event.Type.DELETE, 1, null)
        assertThat(recommendationMessages[0], `is`(sameEventExceptCreatedAt(expectedRecommendationEvent)))

        // Assert one delete review event queued up
        Assertions.assertEquals(1, reviewMessages.size)
        val expectedReviewEvent: Event<Int, Review?> = Event(Event.Type.DELETE, 1, null)
        assertThat(reviewMessages[0], `is`(sameEventExceptCreatedAt(expectedReviewEvent)))
    }

    private fun purgeMessages(bindingName: String) {
        getMessages(bindingName)
    }

    private fun getMessages(bindingName: String): List<String> {
        val messages: MutableList<String> = ArrayList()
        var anyMoreMessages = true
        while (anyMoreMessages) {
            val message: Message<ByteArray>? = getMessage(bindingName)
            if (message == null) {
                anyMoreMessages = false
            } else {
                messages.add(String(message.payload))
            }
        }
        return messages
    }

    private fun getMessage(bindingName: String): Message<ByteArray>? {
        return try {
            target.receive(0, bindingName)
        } catch (npe: NullPointerException) {
            // If the messageQueues member variable in the target object contains no queues when the receive method is called, it will cause a NPE to be thrown.
            // So we catch the NPE here and return null to indicate that no messages were found.
            logger.error { "getMessage() received a NPE with binding = $bindingName" }
            null
        }
    }

    private fun postAndVerifyProduct(compositeProduct: ProductAggregate, expectedStatus: HttpStatus) {
        client.post()
            .uri("/product-composite")
            .body(just(compositeProduct), ProductAggregate::class.java)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
    }

    private fun deleteAndVerifyProduct(productId: Int, expectedStatus: HttpStatus) {
        client.delete()
            .uri("/product-composite/$productId")
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
    }

    companion object : KLogging()
}