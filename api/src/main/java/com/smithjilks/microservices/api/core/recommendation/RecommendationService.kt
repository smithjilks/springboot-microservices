package com.smithjilks.microservices.api.core.recommendation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


interface RecommendationService {
//    /**
//     * Sample usage: curl "$HOST:$PORT/recommendation?productId=1"
//     *
//     * @param productId Id of the product
//     * @return the recommendations of the product
//     */
//    @GetMapping(value = ["/recommendation"], produces = ["application/json"])
//    fun getRecommendations(@RequestParam(value = "productId", required = true) productId: Int): List<Recommendation>
//
//
//    /**
//     * Sample usage, see below.
//     *
//     * curl -X POST $HOST:$PORT/recommendation \
//     * -H "Content-Type: application/json" --data \
//     * '{"productId":123,"recommendationId":456,"author":"me","rate":5,"content":"yada, yada, yada"}'
//     *
//     * @param body A JSON representation of the new recommendation
//     * @return A JSON representation of the newly created recommendation
//     */
//    @PostMapping(value = ["/recommendation"], consumes = ["application/json"], produces = ["application/json"])
//    fun createRecommendation(@RequestBody body: Recommendation): Recommendation?
//
//
//    /**
//     * Sample usage: "curl -X DELETE $HOST:$PORT/recommendation?productId=1".
//     *
//     * @param productId Id of the product
//     */
//    @DeleteMapping(value = ["/recommendation"])
//    fun deleteRecommendations(@RequestParam(value = "productId", required = true) productId: Int)

    fun createRecommendation(body: Recommendation): Mono<Recommendation>?

    /**
     * Sample usage: "curl $HOST:$PORT/recommendation?productId=1".
     *
     * @param productId Id of the product
     * @return the recommendations of the product
     */
    @GetMapping(value = ["/recommendation"], produces = ["application/json"])
    fun getRecommendations(@RequestParam(value = "productId", required = true) productId: Int): Flux<Recommendation>?

    fun deleteRecommendations(productId: Int): Mono<Void>?


}