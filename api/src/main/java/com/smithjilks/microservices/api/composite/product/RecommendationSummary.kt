package com.smithjilks.microservices.api.composite.product

data class RecommendationSummary(
    val recommendationId: Int = 0,
    val author: String,
    val rate: Int = 0,
    val content: String
)