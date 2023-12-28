package com.smithjilks.microservices.api.composite.product

data class ProductAggregate(
    val productId: Int = 0,
    val name: String,
    val weight: Float = 0F,
    val recommendations: List<RecommendationSummary>,
    val reviews: List<ReviewSummary>,
    val serviceAddresses: ServiceAddresses
)