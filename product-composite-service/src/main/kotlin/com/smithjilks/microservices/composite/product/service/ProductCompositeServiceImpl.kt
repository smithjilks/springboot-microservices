package com.smithjilks.microservices.composite.product.service

import com.smithjilks.microservices.api.composite.product.*
import com.smithjilks.microservices.api.core.product.Product
import com.smithjilks.microservices.api.core.recommendation.Recommendation
import com.smithjilks.microservices.api.core.review.Review
import com.smithjilks.microservices.api.exceptions.NotFoundException
import com.smithjilks.microservices.util.ServiceUtil
import org.springframework.web.bind.annotation.RestController


@RestController
class ProductCompositeServiceImpl(
    val serviceUtil: ServiceUtil,
    val productCompositeIntegration: ProductCompositeIntegration
) : ProductCompositeService {

    override fun getProduct(productId: Int): ProductAggregate {
        val product = productCompositeIntegration.getProduct(productId)
            ?: throw NotFoundException("No Product for productId: $productId")
        val recommendations = productCompositeIntegration.getRecommendations(productId)
        val reviews = productCompositeIntegration.getReviews(productId)

        return createProductAggregate(product, recommendations, reviews, serviceUtil.serviceAddress ?: "")
    }

    private fun createProductAggregate(
        product: Product,
        recommendations: List<Recommendation>,
        reviews: List<Review>,
        serviceAddress: String
    ): ProductAggregate {

        // Setup product info
        val productId = product.productId
        val name: String = product.name
        val weight = product.weight


        //Copy summary recommendation info
        val recommendationSummaries = recommendations.map { r ->
            RecommendationSummary(r.recommendationId, r.author, r.rate)
        }

        // Copy review info
        val reviewSummaries = reviews.map { r ->
            ReviewSummary(r.reviewId, r.author, r.subject)
        }

        // create info regarding involved microservices addresses
        val productAddress = product.serviceAddress
        val reviewAddress = if (reviews.isNotEmpty()) reviews.first().serviceAddress else ""
        val recommendationAddress = if (recommendations.isNotEmpty()) recommendations.first().serviceAddress else ""

        val serviceAddresses = ServiceAddresses(
            composite = serviceAddress,
            product = productAddress,
            review = reviewAddress,
            recommendation = recommendationAddress
        )

        return ProductAggregate(
            productId,
            name,
            weight,
            recommendationSummaries,
            reviewSummaries,
            serviceAddresses
        )
    }
}