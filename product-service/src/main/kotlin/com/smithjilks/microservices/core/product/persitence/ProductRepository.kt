package com.smithjilks.microservices.core.product.persitence

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*

interface ProductRepository : PagingAndSortingRepository<ProductEntity, String>,
    MongoRepository<ProductEntity, String> {
    fun findByProductId(productId: Int): Optional<ProductEntity>
}