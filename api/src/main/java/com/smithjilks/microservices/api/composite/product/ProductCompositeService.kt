package com.smithjilks.microservices.api.composite.product

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

interface ProductCompositeService {
    @GetMapping(value = ["/product-composite/{productId}"], produces = ["application/json"])
    fun getProduct(@PathVariable productId: Int): ProductAggregate
}