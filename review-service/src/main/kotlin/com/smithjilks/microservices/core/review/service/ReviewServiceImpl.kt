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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import java.util.logging.Level.FINE


@RestController
class ReviewServiceImpl(
    val serviceUtil: ServiceUtil,
    val repository: ReviewRepository,
    val reviewMapper: ReviewMapper,
    val jdbcScheduler: Scheduler
) : ReviewService {

    companion object : KLogging()

    override fun getReviews(productId: Int): Flux<Review>? {
        if (productId < 1) throw InvalidInputException("Invalid productId: $productId")
        logger.info("Will get reviews for product with id={}", productId)

        return Mono.fromCallable { internalGetReviews(productId) }
            .flatMapMany { Flux.fromIterable(it) }
            .log(logger.name, FINE)
            .subscribeOn(jdbcScheduler)
    }

    private fun internalGetReviews(productId: Int): List<Review> {
        val entityList = repository.findByProductId(productId)
        val reviewList = reviewMapper.entityListToApiList(entityList)
        reviewList.map { review -> review.copy(serviceAddress = serviceUtil.serviceAddress) }
        logger.debug { "Response size: ${reviewList.size}" }
        return reviewList
    }

    override fun createReview(body: Review): Mono<Review>? {
//        return try {
//            val entity: ReviewEntity = reviewMapper.apiToEntity(body)
//            val newEntity = repository.save(entity)
//            logger.debug {
//                "createReview: created a review entity: ProductID/ ReviewID: ${body.productId}/${body.reviewId}"
//            }
//            reviewMapper.entityToApi(newEntity)
//        } catch (dive: DataIntegrityViolationException) {
//            throw InvalidInputException(("Duplicate key, Product Id: ${body.productId} Review Id: ${body.reviewId}"))
//        }

        if (body.productId < 1) throw InvalidInputException("Invalid productId: ${body.productId}")

        return Mono.fromCallable { internalCreateReview(body) }
            .subscribeOn(jdbcScheduler)
    }

    private fun internalCreateReview(body: Review): Review {
        return try {
            val entity: ReviewEntity = reviewMapper.apiToEntity(body)
            val newEntity = repository.save(entity)
            logger.debug { "createReview: created a review entity: ${body.productId}/${body.reviewId}" }
            reviewMapper.entityToApi(newEntity)
        } catch (dive: DataIntegrityViolationException) {
            throw InvalidInputException("Duplicate key, Product Id: ${body.productId}, Review Id: ${body.reviewId}")
        }
    }

    override fun deleteReviews(productId: Int): Mono<Void>? {
//        logger.debug { "deleteReviews: tries to delete reviews for the product with productId: $productId" }
//        repository.deleteAll(repository.findByProductId(productId))

        if (productId < 1) throw InvalidInputException("Invalid productId: $productId")
        return Mono.fromRunnable<Any> { internalDeleteReviews(productId) }.subscribeOn(jdbcScheduler).then()
    }

    private fun internalDeleteReviews(productId: Int) {
        logger.debug { "deleteReviews: tries to delete reviews for the product with productId: $productId" }
        repository.deleteAll(repository.findByProductId(productId))
    }
}