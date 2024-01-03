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


@RestController
class RecommendationServiceImpl(
    val serviceUtil: ServiceUtil,
    val recommendationMapper: RecommendationMapper,
    val repository: RecommendationRepository
) : RecommendationService {

    companion object : KLogging()

    override fun getRecommendations(productId: Int): List<Recommendation> {
        if (productId < 1) {
            throw InvalidInputException("Invalid productId: $productId")
        }

        val entityList = repository.findByProductId(productId)
        val list: List<Recommendation> = recommendationMapper.entityListToApiList(entityList)
        list.map { recommendation -> recommendation.copy(serviceAddress = serviceUtil.serviceAddress ?: "") }

        logger.debug { "getRecommendations: response size: ${list.size}" }

        return list
    }

    override fun createRecommendation(body: Recommendation): Recommendation? {
        return try {
            val entity: RecommendationEntity = recommendationMapper.apiToEntity(body)
            val newEntity = repository.save(entity)
            logger.debug {
                "createRecommendation: created a recommendation entity: ProductID/ RecommendationID ${body.productId}/${body.recommendationId}"
            }
            recommendationMapper.entityToApi(newEntity)
        } catch (dke: DuplicateKeyException) {
            throw InvalidInputException(
                ("Duplicate key, Product Id:  ${body.productId}, Recommendation Id: ${body.recommendationId}")
            )
        }
    }

    override fun deleteRecommendations(productId: Int) {
        logger.debug { "deleteRecommendations: tries to delete recommendations for the product with productId: $productId" }
        repository.deleteAll(repository.findByProductId(productId))
    }
}