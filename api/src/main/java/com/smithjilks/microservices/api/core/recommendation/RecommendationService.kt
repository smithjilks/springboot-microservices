package com.smithjilks.microservices.api.core.recommendation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


interface RecommendationService {
    /**
     * Sample usage: "curl $HOST:$PORT/recommendation?productId=1"
     *
     * @param productId Id of the product
     * @return the recommendations of the product
     */
    @GetMapping(value = ["/recommendation"], produces = ["application/json"])
    fun getRecommendations(@RequestParam(value = "productId", required = true) productId: Int): List<Recommendation>
}