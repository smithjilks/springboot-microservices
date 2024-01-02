package com.smithjilks.microservices.core.product.persitence

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import java.util.*

interface ProductRepository : PagingAndSortingRepository<ProductEntity, String>, CrudRepository<ProductEntity, String> {
    fun findProductById(productId: Int): Optional<ProductEntity>
}