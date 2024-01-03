package com.smithjilks.microservices.api.core.product

import org.springframework.web.bind.annotation.*


interface ProductService {
    /**
     *
     * @param productId Id of the product
     * @return the product, if found, else null
     */
    @GetMapping(value = ["/product/{productId}"], produces = ["application/json"])
    fun getProduct(@PathVariable productId: Int): Product?

    /**
     * Sample usage, see below.
     *
     * curl -X POST $HOST:$PORT/product \
     * -H "Content-Type: application/json" --data \
     * '{"productId":123,"name":"product 123","weight":123}'
     *
     * @param body A JSON representation of the new product
     * @return A JSON representation of the newly created product
     */
    @PostMapping(value = ["/product"], consumes = ["application/json"], produces = ["application/json"])
    fun createProduct(@RequestBody body: Product): Product?


    /**
     * Sample usage: "curl -X DELETE $HOST:$PORT/product/1".
     *
     * @param productId Id of the product
     */
    @DeleteMapping(value = ["/product/{productId}"])
    fun deleteProduct(@PathVariable productId: Int)
}