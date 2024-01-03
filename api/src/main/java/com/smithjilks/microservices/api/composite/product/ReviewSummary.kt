package com.smithjilks.microservices.api.composite.product

data class ReviewSummary(
    val reviewId: Int = 0,
    val author: String,
    val subject: String,
    val content: String
)