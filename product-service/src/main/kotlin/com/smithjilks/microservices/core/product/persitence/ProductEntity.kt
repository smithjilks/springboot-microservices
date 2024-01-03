package com.smithjilks.microservices.core.product.persitence

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "products")
data class ProductEntity(
    @Id
    val id: String? = null,
    @Version
    val version: Int? = null,
    @Indexed(unique = true)
    val productId: Int,
    val name: String?,
    val weight: Float
)