package com.smithjilks.microservices.core.recommendation

import com.smithjilks.microservices.core.recommendation.persistence.RecommendationEntity
import com.smithjilks.microservices.core.recommendation.persistence.RecommendationRepository
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException


@DataMongoTest
internal class PersistenceTests : MongoDbTestBase() {
    @Autowired
    lateinit var repository: RecommendationRepository

    private lateinit var savedEntity: RecommendationEntity

    @BeforeEach
    fun setupDb() {
        repository.deleteAll().block()
        val entity = RecommendationEntity("1", null, 1, 1, "a", 3, "c")
        savedEntity = repository.save(entity).block()!!
        assertEqualsRecommendation(entity, savedEntity)
    }

    @Test
    fun create() {
        val newEntity = RecommendationEntity("4", null, 3, 3, "a", 3, "c")
        repository.save(newEntity).block()
        val foundEntity = repository.findById(newEntity.id!!).block()
        assertEqualsRecommendation(newEntity, foundEntity!!)
        Assertions.assertEquals(2, repository.count().block())
    }

    @Test
    fun update() {
        repository.save(savedEntity.copy(author = "a2")).block()
        val (_, version, _, _, author) = repository.findById(savedEntity.id!!).block()!!
        Assertions.assertEquals(1, version!!.toLong())
        Assertions.assertEquals("a2", author)
    }

    @Test
    fun delete() {
        repository.delete(savedEntity).block()
        repository.existsById(savedEntity.id!!).block()?.let { Assertions.assertFalse(it) }
    }

    @Test
    fun getByProductId() {
        val entityList = repository.findByProductId(savedEntity.productId).collectList().block()
        assertThat(entityList, hasSize(1))
        assertEqualsRecommendation(savedEntity, entityList?.get(0)!!)
    }

    @Test
    fun duplicateError() {
        Assertions.assertThrows(DuplicateKeyException::class.java) {
            val entity = RecommendationEntity("1", null, 1, 2, "a", 3, "c")
            repository.save<RecommendationEntity>(entity).block()
        }
    }

    @Test
    fun optimisticLockError() {
        // Store the saved entity in two separate entity objects
        val entity1 = repository.findById(savedEntity.id!!).block()
        val entity2 = repository.findById(savedEntity.id!!).block()

        // Update the entity using the first entity object
        repository.save(entity1!!.copy(author = "a1")).block()

        //  Update the entity using the second entity object.
        // This should fail since the second entity now holds an old version number, i.e. an Optimistic Lock Error
        Assertions.assertThrows(OptimisticLockingFailureException::class.java) {
            repository.save<RecommendationEntity>(entity2!!.copy(author = "a2")).block()
        }

        // Get the updated entity from the database and verify its new state
        val (_, version, _, _, author) = repository.findById(savedEntity.id!!).block()!!
        Assertions.assertEquals(1, version as Int)
        Assertions.assertEquals("a1", author)
    }

    private fun assertEqualsRecommendation(expectedEntity: RecommendationEntity, actualEntity: RecommendationEntity) {
        Assertions.assertEquals(expectedEntity.id, actualEntity.id)
        Assertions.assertEquals(expectedEntity.version ?: 0, actualEntity.version)
        Assertions.assertEquals(expectedEntity.productId, actualEntity.productId)
        Assertions.assertEquals(expectedEntity.recommendationId, actualEntity.recommendationId)
        Assertions.assertEquals(expectedEntity.author, actualEntity.author)
        Assertions.assertEquals(expectedEntity.rating, actualEntity.rating)
        Assertions.assertEquals(expectedEntity.content, actualEntity.content)
    }
}