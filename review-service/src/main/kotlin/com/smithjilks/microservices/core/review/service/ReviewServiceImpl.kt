package com.smithjilks.microservices.core.review.service

import com.smithjilks.microservices.api.core.review.Review
import com.smithjilks.microservices.api.core.review.ReviewService
import com.smithjilks.microservices.api.exceptions.InvalidInputException
import com.smithjilks.microservices.core.review.persistence.ReviewEntity
import com.smithjilks.microservices.core.review.persistence.ReviewRepository
import com.smithjilks.microservices.util.ServiceUtil
import mu.KLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.web.bind.annotation.RestController


@RestController
class ReviewServiceImpl(
    val serviceUtil: ServiceUtil,
    val repository: ReviewRepository,
    val reviewMapper: ReviewMapper
) : ReviewService {

    companion object : KLogging()

    override fun getReviews(productId: Int): List<Review> {
        if (productId < 1) {
            throw InvalidInputException("Invalid productId: $productId")
        }

        val entityList = repository.findByProductId(productId)
        val list: List<Review> = reviewMapper.entityListToApiList(entityList)

        list.map { review -> review.copy(serviceAddress = serviceUtil.serviceAddress ?: "") }

        logger.debug { "getReviews: response size: $list.size" }
        return list
    }

    override fun createReview(body: Review): Review? {
        return try {
            val entity: ReviewEntity = reviewMapper.apiToEntity(body)
            val newEntity = repository.save(entity)
            logger.debug {
                "createReview: created a review entity: ProductID/ ReviewID: ${body.productId}/${body.reviewId}"
            }
            reviewMapper.entityToApi(newEntity)
        } catch (dive: DataIntegrityViolationException) {
            throw InvalidInputException(("Duplicate key, Product Id: ${body.productId} Review Id: ${body.reviewId}"))
        }
    }

    override fun deleteReviews(productId: Int) {
        logger.debug { "deleteReviews: tries to delete reviews for the product with productId: $productId" }
        repository.deleteAll(repository.findByProductId(productId))
    }
}