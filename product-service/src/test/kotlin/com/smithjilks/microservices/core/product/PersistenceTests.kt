package com.smithjilks.microservices.core.product

import com.smithjilks.microservices.core.product.persitence.ProductEntity
import com.smithjilks.microservices.core.product.persitence.ProductRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Direction.ASC
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream.rangeClosed


@DataMongoTest
class PersistenceTests : MongoDbTestBase() {
    @Autowired
    lateinit var repository: ProductRepository

    private lateinit var savedEntity: ProductEntity

    @BeforeEach
    fun setupDb() {
        repository.deleteAll()

        val entity = ProductEntity("1", null, 1, "cup", 4.5F)
        savedEntity = repository.save(entity)

        assertEqualsProduct(entity, savedEntity)
    }

    @Test
    fun create() {
        val newEntity = ProductEntity("3", null, 2, "plate", 9.5F)

        val savedEntity = repository.save(newEntity)
        val foundEntity = repository.findById(savedEntity.id!!).get()
        assertEqualsProduct(savedEntity, foundEntity)
        Assertions.assertEquals(2, repository.count())
    }

    @Test
    fun update() {
        val updated = savedEntity.copy(name = "n2")
        repository.save(updated)
        val (_, version, _, name) = repository.findById(savedEntity.id!!).get()
        Assertions.assertEquals(1, version?.toLong())
        Assertions.assertEquals("n2", name)
    }

    @Test
    fun delete() {
        repository.delete(savedEntity)
        Assertions.assertFalse(repository.existsById(savedEntity.id!!))
    }

    @Test
    fun getByProductId() {
        val entity: Optional<ProductEntity> = repository.findByProductId(savedEntity.productId)
        Assertions.assertTrue(entity.isPresent)
        assertEqualsProduct(savedEntity, entity.get())
    }

    @Test
    fun duplicateError() {
        Assertions.assertThrows(DuplicateKeyException::class.java) {
            val entity = ProductEntity(savedEntity.id.toString(), null, 6, "n7", 1.5F)
            repository.save<ProductEntity>(entity)
        }
    }

    @Test
    fun optimisticLockError() {

        // Store the saved entity in two separate entity objects
        val entity1 = repository.findById(savedEntity.id!!).get()
        val entity2 = repository.findById(savedEntity.id!!).get()

        // Update the entity using the first entity object
        repository.save(entity1.copy(name = "n1"))

        // Update the entity using the second entity object.
        // This should fail since the second entity now holds an old version number, i.e. an Optimistic Lock Error
        Assertions.assertThrows(OptimisticLockingFailureException::class.java) {
            repository.save<ProductEntity>(entity2.copy(name = "n2"))
        }

        // Get the updated entity from the database and verify its new sate
        val (_, version, _, name) = repository.findById(savedEntity.id!!).get()
        Assertions.assertEquals(1, version)
        Assertions.assertEquals("n1", name)
    }

    @Test
    fun paging() {
        repository.deleteAll()
        val newProducts: List<ProductEntity> = rangeClosed(1001, 1010)
            .mapToObj { i -> ProductEntity("_$i", null, i, "name $i", i.toFloat()) }
            .collect(Collectors.toList())
        repository.saveAll(newProducts)
        var nextPage: Pageable = PageRequest.of(0, 4, ASC, "productId")
        nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true)
        nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true)
        testNextPage(nextPage, "[1009, 1010]", false)
    }


    private fun testNextPage(nextPage: Pageable, expectedProductIds: String, expectsNextPage: Boolean): Pageable {
        val productPage: Page<ProductEntity> = repository.findAll(nextPage)
        Assertions.assertEquals(
            expectedProductIds,
            productPage.content.map { p -> p.productId }.toList().toString()
        )
        Assertions.assertEquals(expectsNextPage, productPage.hasNext())
        return productPage.nextPageable()
    }

    private fun assertEqualsProduct(expectedEntity: ProductEntity, actualEntity: ProductEntity) {
        Assertions.assertEquals(expectedEntity.id, actualEntity.id)
        Assertions.assertEquals(expectedEntity.version ?: 0, actualEntity.version)
        Assertions.assertEquals(expectedEntity.productId, actualEntity.productId)
        Assertions.assertEquals(expectedEntity.name, actualEntity.name)
        Assertions.assertEquals(expectedEntity.weight, actualEntity.weight)
    }
}