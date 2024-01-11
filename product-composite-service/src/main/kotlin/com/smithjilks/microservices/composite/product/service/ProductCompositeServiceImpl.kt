package com.smithjilks.microservices.composite.product.service

import com.smithjilks.microservices.api.composite.product.*
import com.smithjilks.microservices.api.core.product.Product
import com.smithjilks.microservices.api.core.recommendation.Recommendation
import com.smithjilks.microservices.api.core.review.Review
import com.smithjilks.microservices.util.ServiceUtil
import mu.KLogging
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.logging.Level.FINE


@RestController
class ProductCompositeServiceImpl(
    val serviceUtil: ServiceUtil,
    val productCompositeIntegration: ProductCompositeIntegration
) : ProductCompositeService {

    companion object : KLogging()

    override fun getProduct(productId: Int): Mono<ProductAggregate> {
//        val product = productCompositeIntegration.getProduct(productId)
//            ?: throw NotFoundException("No Product for productId: $productId")
//        val recommendations = productCompositeIntegration.getRecommendations(productId)
//        val reviews = productCompositeIntegration.getReviews(productId)
//
//        return createProductAggregate(product, recommendations, reviews, serviceUtil.serviceAddress ?: "")

        logger.info { "Will get composite product info for product.id=$productId" }
        return Mono.zip(
            { values: Array<Any?> ->
                createProductAggregate(
                    (values[0] as Product?)!!,
                    values[1] as List<Recommendation>,
                    values[2] as List<Review>,
                    serviceUtil.serviceAddress!!
                )
            },
            productCompositeIntegration.getProduct(productId),
            productCompositeIntegration.getRecommendations(productId).collectList(),
            productCompositeIntegration.getReviews(productId).collectList()
        )
            .doOnError { ex: Throwable ->
                logger.warn { "getCompositeProduct failed: $ex" }
            }
            .log(logger.name, FINE)
    }

    override fun createProduct(body: ProductAggregate): Mono<Void> {
//        try {
//            logger.debug { "createCompositeProduct: creates a new composite entity for productId: ${body.productId}" }
//            val product = Product(body.productId, body.name, body.weight, "")
//            productCompositeIntegration.createProduct(product)
//
//            body.recommendations.forEach { r ->
//                val recommendation = Recommendation(
//                    body.productId,
//                    r.recommendationId,
//                    r.author,
//                    r.rate,
//                    r.content,
//                    ""
//                )
//                productCompositeIntegration.createRecommendation(recommendation)
//            }
//
//            body.reviews.forEach { r ->
//                val review =
//                    Review(
//                        body.productId,
//                        r.reviewId,
//                        r.author,
//                        r.subject,
//                        r.content,
//                        ""
//                    )
//                productCompositeIntegration.createReview(review)
//            }
//            logger.debug { "createCompositeProduct: composite entities created for productId: ${body.productId}" }
//        } catch (re: RuntimeException) {
//            logger.warn { "createCompositeProduct failed: $re" }
//            throw re
//        }

        return try {
            val monoList: MutableList<Mono<*>> = ArrayList()
            logger.info { "Will create a new composite entity for product.id: ${body.productId}" }
            val product = Product(body.productId, body.name, body.weight, null)
            productCompositeIntegration.createProduct(product)?.let { monoList.add(it) }
            if (body.recommendations != null) {
                body.recommendations.forEach { r ->
                    val recommendation = Recommendation(
                        body.productId,
                        r.recommendationId,
                        r.author,
                        r.rate,
                        r.content,
                        null
                    )
                    productCompositeIntegration.createRecommendation(recommendation)?.let { monoList.add(it) }
                }
            }
            if (body.reviews != null) {
                body.reviews.forEach { r ->
                    val review =
                        Review(
                            body.productId,
                            r.reviewId,
                            r.author,
                            r.subject,
                            r.content,
                            null
                        )
                    productCompositeIntegration.createReview(review)?.let { monoList.add(it) }
                }
            }
            logger.debug("createCompositeProduct: composite entities created for productId: {}", body.productId)
            Mono.zip({ r: Array<Any?>? -> "" }, *monoList.toTypedArray<Mono<*>>())
                .doOnError { ex: Throwable ->
                    logger.warn { "createCompositeProduct failed: $ex" }
                }
                .then()
        } catch (re: RuntimeException) {
            logger.warn { "createCompositeProduct failed: $re" }
            throw re
        }
    }

    override fun deleteProduct(productId: Int): Mono<Void> {

//        logger.debug { "deleteCompositeProduct: Deletes a product aggregate for productId: $productId" }
//
//        productCompositeIntegration.deleteProduct(productId)
//
//        productCompositeIntegration.deleteRecommendations(productId)
//
//        productCompositeIntegration.deleteReviews(productId)
//
//        logger.debug { "deleteCompositeProduct: aggregate entities deleted for productId: $productId" }

        return try {
            logger.info { "Will delete a product aggregate for product.id: $productId" }
            Mono.zip(
                { r: Array<Any?>? -> "" },
                productCompositeIntegration.deleteProduct(productId),
                productCompositeIntegration.deleteRecommendations(productId),
                productCompositeIntegration.deleteReviews(productId)
            ).doOnError { ex: Throwable ->
                logger.warn { "delete failed: $ex" }
            }
                .log(logger.name, FINE).then()
        } catch (re: java.lang.RuntimeException) {
            logger.warn { "deleteCompositeProduct failed: $re" }
            throw re
        }
    }

    private fun createProductAggregate(
        product: Product,
        recommendations: List<Recommendation>,
        reviews: List<Review>,
        serviceAddress: String
    ): ProductAggregate {

        // Setup product info
        val productId = product.productId
        val name = product.name
        val weight = product.weight


        //Copy summary recommendation info
        val recommendationSummaries = recommendations.map { r ->
            RecommendationSummary(r.recommendationId, r.author, r.rate, r.content)
        }

        // Copy review info
        val reviewSummaries = reviews.map { r ->
            ReviewSummary(r.reviewId, r.author, r.subject, r.content)
        }

        // create info regarding involved microservices addresses
        val productAddress = product.serviceAddress ?: ""
        val reviewAddress = if (reviews.isNotEmpty()) reviews.first().serviceAddress ?: "" else ""
        val recommendationAddress =
            if (recommendations.isNotEmpty()) recommendations.first().serviceAddress ?: "" else ""

        val serviceAddresses = ServiceAddresses(
            composite = serviceAddress,
            product = productAddress,
            review = reviewAddress,
            recommendation = recommendationAddress
        )

        return ProductAggregate(
            productId,
            name ?: "",
            weight,
            recommendationSummaries,
            reviewSummaries,
            serviceAddresses
        )
    }
}