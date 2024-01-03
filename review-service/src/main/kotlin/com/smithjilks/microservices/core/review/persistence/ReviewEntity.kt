package com.smithjilks.microservices.core.review.persistence

import jakarta.persistence.*

@Entity
@Table(
    name = "reviews",
    indexes = [Index(name = "reviews_unique_idx", unique = true, columnList = "productId, reviewId")]
)
data class ReviewEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int,
    @Version
    val version: Int,

    val productId: Int,
    val reviewId: Int,
    val author: String,
    val subject: String,
    val content: String,
)
