package com.smithjilks.microservices.api.core.product

data class Product(
    val productId: Int,
    val name: String?,
    val weight: Float,
    val serviceAddress: String?,
)