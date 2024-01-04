package com.smithjilks.microservices.core.review

import com.smithjilks.microservices.core.review.persistence.ReviewEntity
import com.smithjilks.microservices.core.review.persistence.ReviewRepository
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenceTests : PostgreSqlTestBase() {

    @Autowired
    lateinit var repository: ReviewRepository

    private lateinit var savedEntity: ReviewEntity

    @BeforeEach
    fun setupDb() {
        repository.deleteAll()

        val entity = ReviewEntity(1, 1, 1, 1, "a", "s", "c")
        savedEntity = repository.save(entity)

        assertEqualsReview(entity, savedEntity)
    }

    @Test
    fun create() {
        val newEntity = ReviewEntity(2, 1, 2, 2, "a", "s", "c")
        repository.save(newEntity)
        val foundEntity = repository.findById(newEntity.id).get()
        assertEqualsReview(newEntity, foundEntity)
        Assertions.assertEquals(2, repository.count())
    }

    @Test
    fun update() {
        val updatedEntity = savedEntity.copy(author = "a2")
        repository.save(updatedEntity)
        val (_, version, _, _, author) = repository.findById(updatedEntity.id).get()
        Assertions.assertEquals(2, version)
        Assertions.assertEquals("a2", author)
    }

    @Test
    fun delete() {
        repository.delete(savedEntity)
        Assertions.assertFalse(repository.existsById(savedEntity.id))
    }

    @Test
    fun getByProductId() {
        val entityList = repository.findByProductId(savedEntity.productId)
        assertThat(entityList, hasSize(1))
        assertEqualsReview(savedEntity, entityList[0])
    }

    @Test
    fun duplicateError() {
        Assertions.assertThrows(DataIntegrityViolationException::class.java) {
            val entity = ReviewEntity(1, 1, 1, 1, "a", "s", "c")
            repository.save<ReviewEntity>(entity)
        }
    }

    @Test
    fun optimisticLockError() {

        // Store the saved entity in two separate entity objects
        val entity1 = repository.findById(savedEntity.id).get()
        val entity2 = repository.findById(savedEntity.id).get()

        // Update the entity using the first entity object
        repository.save(entity1.copy(author = "a1"))

        // Update the entity using the second entity object.
        // This should fail since the second entity now holds an old version number, i.e. an Optimistic Lock Error
        Assertions.assertThrows(OptimisticLockingFailureException::class.java) {
            repository.save<ReviewEntity>(entity2.copy(author = "a2"))
        }

        // Get the updated entity from the database and verify its new state
        val (_, version, _, _, author) = repository.findById(savedEntity.id).get()
        Assertions.assertEquals(2, version)
        Assertions.assertEquals("a1", author)
    }


    private fun assertEqualsReview(expectedEntity: ReviewEntity, actualEntity: ReviewEntity) {
        Assertions.assertEquals(expectedEntity.id, actualEntity.id)
        Assertions.assertEquals(expectedEntity.version, actualEntity.version)
        Assertions.assertEquals(expectedEntity.productId, actualEntity.productId)
        Assertions.assertEquals(expectedEntity.reviewId, actualEntity.reviewId)
        Assertions.assertEquals(expectedEntity.author, actualEntity.author)
        Assertions.assertEquals(expectedEntity.subject, actualEntity.subject)
        Assertions.assertEquals(expectedEntity.content, actualEntity.content)
    }


}