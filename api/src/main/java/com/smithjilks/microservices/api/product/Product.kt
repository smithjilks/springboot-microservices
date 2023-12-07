package com.smithjilks.microservices.api.product

data class Product(
    val productId: Int,
    val name: String,
    val weight: Int,
    val serviceAddress: String,
)