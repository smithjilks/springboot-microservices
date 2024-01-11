package com.smithjilks.microservices.core.product.persitence

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface ProductRepository : ReactiveCrudRepository<ProductEntity, String> {
    fun findByProductId(productId: Int): Mono<ProductEntity>
}