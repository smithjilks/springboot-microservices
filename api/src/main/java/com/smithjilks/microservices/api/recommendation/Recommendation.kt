package com.smithjilks.microservices.api.recommendation;


data class Recommendation(
    val productId: Int,
    val recommendationId: Int,
    val author: String,
    val rate: Int,
    val content: String,
    val serviceAddress: String
)