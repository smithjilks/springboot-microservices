package com.smithjilks.microservices.core.product

import com.smithjilks.microservices.api.core.product.Product
import com.smithjilks.microservices.api.event.Event
import com.smithjilks.microservices.api.exceptions.InvalidInputException
import com.smithjilks.microservices.core.product.persitence.ProductRepository
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
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.Mono.just
import java.util.function.Consumer


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "36000")
@Testcontainers
class ProductServiceApplicationTests : MongoDbTestBase() {

    @Autowired
    lateinit var client: WebTestClient

    @Autowired
    lateinit var repository: ProductRepository

    @Autowired
    @Qualifier("messageProcessor")
    lateinit var messageProcessor: Consumer<Event<Int, Product?>>

    @BeforeEach
    fun setupDb() {
//        repository.deleteAll()
        repository.deleteAll().block()
    }

    @Test
    fun getProductById() {
        val productId = 1
//        postAndVerifyProduct(productId, HttpStatus.OK)
//        Assertions.assertTrue(repository.findByProductId(productId).isPresent)
//        getAndVerifyProduct(productId, HttpStatus.OK).jsonPath("$.productId").isEqualTo(productId)

        Assertions.assertNull(repository.findByProductId(productId).block())
        Assertions.assertEquals(0, repository.count().block() as Long)

        sendCreateProductEvent(productId)

        Assertions.assertNotNull(repository.findByProductId(productId).block())
        Assertions.assertEquals(1, repository.count().block() as Long)

        getAndVerifyProduct(productId, HttpStatus.OK)
            .jsonPath("$.productId").isEqualTo(productId)

    }

    @Test
    fun duplicateError() {
        val productId = 1
//        postAndVerifyProduct(productId, HttpStatus.OK)
//        Assertions.assertTrue(repository.findByProductId(productId).isPresent)
//        postAndVerifyProduct(productId, HttpStatus.UNPROCESSABLE_ENTITY)
//            .jsonPath("$.path").isEqualTo("/product")
//            .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: $productId")

        Assertions.assertNull(repository.findByProductId(productId).block())

        sendCreateProductEvent(productId)

        Assertions.assertNotNull(repository.findByProductId(productId).block())

        val thrown: InvalidInputException = assertThrows(
            InvalidInputException::class.java,
            { sendCreateProductEvent(productId) },
            "Expected a InvalidInputException here!"
        )
        Assertions.assertEquals("Duplicate key, Product Id: $productId", thrown.message)
    }

    @Test
    fun deleteProduct() {
        val productId = 1
//        postAndVerifyProduct(productId, HttpStatus.OK)
//        Assertions.assertTrue(repository.findByProductId(productId).isPresent)
//        deleteAndVerifyProduct(productId, HttpStatus.OK)
//        Assertions.assertFalse(repository.findByProductId(productId).isPresent)
//        deleteAndVerifyProduct(productId, HttpStatus.OK)

        sendCreateProductEvent(productId)
        Assertions.assertNotNull(repository.findByProductId(productId).block())

        sendDeleteProductEvent(productId)
        Assertions.assertNull(repository.findByProductId(productId).block())

        sendDeleteProductEvent(productId)
    }

    @Test
    fun getProductInvalidParameterString() {
        getAndVerifyProduct("/no-integer", HttpStatus.BAD_REQUEST)
            .jsonPath("$.path").isEqualTo("/product/no-integer")
            .jsonPath("$.message").isEqualTo("Type mismatch.")
    }

    @Test
    fun getProductNotFound() {
        val productIdNotFound = 13
        getAndVerifyProduct(productIdNotFound, HttpStatus.NOT_FOUND)
            .jsonPath("$.path").isEqualTo("/product/$productIdNotFound")
            .jsonPath("$.message").isEqualTo("No product found for productId: $productIdNotFound")
    }

    @Test
    fun getProductInvalidParameterNegativeValue() {
        val productIdInvalid = -1
        getAndVerifyProduct(productIdInvalid, HttpStatus.UNPROCESSABLE_ENTITY)
            .jsonPath("$.path").isEqualTo("/product/$productIdInvalid")
            .jsonPath("$.message").isEqualTo("Invalid productId: $productIdInvalid")
    }

    private fun getAndVerifyProduct(productId: Int, expectedStatus: HttpStatus): BodyContentSpec {
        return getAndVerifyProduct("/$productId", expectedStatus)
    }

    private fun getAndVerifyProduct(productIdPath: String, expectedStatus: HttpStatus): BodyContentSpec {
        return client.get()
            .uri("/product$productIdPath")
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectHeader().contentType(APPLICATION_JSON)
            .expectBody()
    }

    private fun postAndVerifyProduct(productId: Int, expectedStatus: HttpStatus): BodyContentSpec {
        val product = Product(
            productId,
            "Name $productId", productId.toFloat(), "SA"
        )
        return client.post()
            .uri("/product")
            .body(just(product), Product::class.java)
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectHeader().contentType(APPLICATION_JSON)
            .expectBody()
    }

    private fun deleteAndVerifyProduct(productId: Int, expectedStatus: HttpStatus): BodyContentSpec {
        return client.delete()
            .uri("/product/$productId")
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(expectedStatus)
            .expectBody()
    }

    private fun sendCreateProductEvent(productId: Int) {
        val product = Product(
            productId,
            "Name $productId", productId.toFloat(), "SA"
        )
        val event: Event<Int, Product?> = Event(Event.Type.CREATE, productId, product)
        messageProcessor.accept(event)
    }

    private fun sendDeleteProductEvent(productId: Int) {
        val event: Event<Int, Product?> =
            Event(Event.Type.DELETE, productId, null)
        messageProcessor.accept(event)
    }

}
