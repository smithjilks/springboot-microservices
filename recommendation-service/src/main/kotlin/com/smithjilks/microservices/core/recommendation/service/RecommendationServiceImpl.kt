package com.smithjilks.microservices.core.recommendation.service

import com.smithjilks.microservices.api.core.recommendation.Recommendation
import com.smithjilks.microservices.api.core.recommendation.RecommendationService
import com.smithjilks.microservices.api.exceptions.InvalidInputException
import com.smithjilks.microservices.core.recommendation.persistence.RecommendationEntity
import com.smithjilks.microservices.core.recommendation.persistence.RecommendationRepository
import com.smithjilks.microservices.util.ServiceUtil
import mu.KLogging
import org.springframework.dao.DuplicateKeyException
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.logging.Level.FINE


@RestController
class RecommendationServiceImpl(
    val serviceUtil: ServiceUtil,
    val recommendationMapper: RecommendationMapper,
    val repository: RecommendationRepository
) : RecommendationService {

    companion object : KLogging()

    override fun getRecommendations(productId: Int): Flux<Recommendation>? {
//        if (productId < 1) {
//            throw InvalidInputException("Invalid productId: $productId")
//        }
//
//        val entityList = repository.findByProductId(productId)
//        val list: List<Recommendation> = recommendationMapper.entityListToApiList(entityList)
//        list.map { recommendation -> recommendation.copy(serviceAddress = serviceUtil.serviceAddress ?: "") }
//
//        logger.debug { "getRecommendations: response size: ${list.size}" }
//
//        return list

        if (productId < 1) throw InvalidInputException("Invalid productId: $productId")


        logger.info { "Will get recommendations for product with id = $productId" }

        return repository.findByProductId(productId)
            .log(logger.name, FINE)
            .map { recommendationEntity -> recommendationMapper.entityToApi(recommendationEntity) }
            .map { recommendation -> setServiceAddress(recommendation) }
    }

    override fun createRecommendation(body: Recommendation): Mono<Recommendation>? {
//        return try {
//            val entity: RecommendationEntity = recommendationMapper.apiToEntity(body)
//            val newEntity = repository.save(entity)
//            logger.debug {
//                "createRecommendation: created a recommendation entity: ProductID/ RecommendationID ${body.productId}/${body.recommendationId}"
//            }
//            recommendationMapper.entityToApi(newEntity)
//        } catch (dke: DuplicateKeyException) {
//            throw InvalidInputException(
//                ("Duplicate key, Product Id:  ${body.productId}, Recommendation Id: ${body.recommendationId}")
//            )
//        }

        if (body.productId < 1) throw InvalidInputException("Invalid productId: ${body.productId}")

        val recommendationEntity = recommendationMapper.apiToEntity(body)

        return repository.save<RecommendationEntity>(recommendationEntity)
            .log(logger.name, FINE)
            .onErrorMap(DuplicateKeyException::class.java) { ex: DuplicateKeyException ->
                InvalidInputException("Duplicate key, Product Id: ${body.productId}, Recommendation Id: ${body.recommendationId}")
            }
            .map { entity -> recommendationMapper.entityToApi(entity) }

    }

    override fun deleteRecommendations(productId: Int): Mono<Void>? {
//        logger.debug { "deleteRecommendations: tries to delete recommendations for the product with productId: $productId" }
//        repository.deleteAll(repository.findByProductId(productId))

        if (productId < 1) throw InvalidInputException("Invalid productId: " + productId)

        logger.debug { "deleteRecommendations: tries to delete recommendations for the product with productId: $productId" }
        return repository.deleteAll(repository.findByProductId(productId))
    }

    private fun setServiceAddress(recommendation: Recommendation): Recommendation {
        return recommendation.copy(serviceAddress = serviceUtil.serviceAddress)
    }
}