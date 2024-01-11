package com.smithjilks.microservices.core.product

import com.smithjilks.microservices.core.product.persitence.ProductEntity
import com.smithjilks.microservices.core.product.persitence.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import reactor.test.StepVerifier


@DataMongoTest
class PersistenceTests : MongoDbTestBase() {
    @Autowired
    lateinit var repository: ProductRepository

    private lateinit var savedEntity: ProductEntity

    @BeforeEach
    fun setupDb() {
        StepVerifier.create(repository.deleteAll()).verifyComplete()

        val entity = ProductEntity("1", null, 1, "cup", 4.5F)
        StepVerifier.create(repository.save<ProductEntity>(entity))
            .expectNextMatches { createdEntity: ProductEntity ->
                savedEntity = createdEntity
                areProductEqual(entity, savedEntity)
            }
            .verifyComplete()
    }

    @Test
    fun create() {
        val newEntity = ProductEntity("3", null, 2, "plate", 9.5F)

        StepVerifier.create(repository.save(newEntity))
            .expectNextMatches { (_, _, productId): ProductEntity -> newEntity.productId == productId }
            .verifyComplete()

        StepVerifier.create(repository.findById(newEntity.id!!))
            .expectNextMatches { foundEntity: ProductEntity? ->
                areProductEqual(
                    newEntity,
                    foundEntity!!
                )
            }
            .verifyComplete()

        StepVerifier.create(repository.count()).expectNext(2).verifyComplete()
    }

    @Test
    fun update() {
        val updated = savedEntity.copy(name = "n2")
        repository.save(updated)
        StepVerifier.create(repository.save(updated))
            .expectNextMatches { (_, _, _, name): ProductEntity -> name == "n2" }
            .verifyComplete()

        StepVerifier.create(repository.findById(savedEntity.id!!))
            .expectNextMatches { (_, version, _, name): ProductEntity ->
                version == 1 && name == "n2"
            }
            .verifyComplete()
    }

    @Test
    fun delete() {
        StepVerifier.create(repository.delete(savedEntity)).verifyComplete()
        StepVerifier.create(repository.existsById(savedEntity.id!!)).expectNext(false).verifyComplete()

    }

    @Test
    fun getByProductId() {
        StepVerifier.create(repository.findByProductId(savedEntity.productId))
            .expectNextMatches { foundEntity: ProductEntity? ->
                areProductEqual(
                    savedEntity,
                    foundEntity!!
                )
            }
            .verifyComplete()
    }

    @Test
    fun duplicateError() {
        val entity = ProductEntity(savedEntity.id.toString(), null, 6, "n7", 1.5F)
        StepVerifier.create(repository.save(entity)).expectError(DuplicateKeyException::class.java).verify()
    }

    @Test
    fun optimisticLockError() {

        // Store the saved entity in two separate entity objects
        val entity1 = repository.findById(savedEntity.id!!).block()
        val entity2 = repository.findById(savedEntity.id!!).block()

        // Update the entity using the first entity object
        entity1?.let {
            repository.save(it.copy(name = "n1")).block()
        }

        // Update the entity using the second entity object.
        // This should fail since the second entity now holds an old version number, i.e. an Optimistic Lock Error
        StepVerifier.create(repository.save(entity2!!.copy(name = "n2"))).expectError(
            OptimisticLockingFailureException::class.java
        ).verify()


        // Get the updated entity from the database and verify its new state
        StepVerifier.create(repository.findById(savedEntity.id!!))
            .expectNextMatches { (_, version, _, name): ProductEntity ->
                version == 1 && name == "n1"
            }
            .verifyComplete()
    }

    private fun areProductEqual(expectedEntity: ProductEntity, actualEntity: ProductEntity): Boolean {
        return (expectedEntity.id == actualEntity.id
                && ((expectedEntity.version ?: 0) === actualEntity.version)
                && expectedEntity.productId == actualEntity.productId
                && expectedEntity.name == actualEntity.name
                && expectedEntity.weight == actualEntity.weight
                )
    }
}