package com.smithjilks.microservices.core.product

import com.smithjilks.microservices.api.core.product.Product
import com.smithjilks.microservices.core.product.persitence.ProductRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.Mono.just


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "36000")
@Testcontainers
class ProductServiceApplicationTests : MongoDbTestBase() {

    @Autowired
    lateinit var client: WebTestClient

    @Autowired
    lateinit var repository: ProductRepository

    @BeforeEach
    fun setupDb() {
        repository.deleteAll()
    }

    @Test
    fun getProductById() {
        val productId = 1
        postAndVerifyProduct(productId, HttpStatus.OK)
        Assertions.assertTrue(repository.findByProductId(productId).isPresent)
        getAndVerifyProduct(productId, HttpStatus.OK).jsonPath("$.productId").isEqualTo(productId)
    }

    @Test
    fun duplicateError() {
        val productId = 1
        postAndVerifyProduct(productId, HttpStatus.OK)
        Assertions.assertTrue(repository.findByProductId(productId).isPresent)
        postAndVerifyProduct(productId, HttpStatus.UNPROCESSABLE_ENTITY)
            .jsonPath("$.path").isEqualTo("/product")
            .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: $productId")
    }

    @Test
    fun deleteProduct() {
        val productId = 1
        postAndVerifyProduct(productId, HttpStatus.OK)
        Assertions.assertTrue(repository.findByProductId(productId).isPresent)
        deleteAndVerifyProduct(productId, HttpStatus.OK)
        Assertions.assertFalse(repository.findByProductId(productId).isPresent)
        deleteAndVerifyProduct(productId, HttpStatus.OK)
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

}
