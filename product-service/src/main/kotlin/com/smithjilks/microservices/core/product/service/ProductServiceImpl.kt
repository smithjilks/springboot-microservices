package com.smithjilks.microservices.core.product.service

import com.smithjilks.microservices.api.core.product.Product
import com.smithjilks.microservices.api.core.product.ProductService
import com.smithjilks.microservices.api.exceptions.InvalidInputException
import com.smithjilks.microservices.api.exceptions.NotFoundException
import com.smithjilks.microservices.core.product.persitence.ProductEntity
import com.smithjilks.microservices.core.product.persitence.ProductRepository
import com.smithjilks.microservices.util.ServiceUtil
import mu.KLogging
import org.springframework.dao.DuplicateKeyException
import org.springframework.web.bind.annotation.RestController


@RestController
class ProductServiceImpl(
    val serviceUtil: ServiceUtil,
    val repository: ProductRepository,
    val productMapper: ProductMapper,
) : ProductService {

    companion object : KLogging()

    override fun getProduct(productId: Int): Product? {
        if (productId < 1) {
            throw InvalidInputException("Invalid productId: $productId")
        }

        val entity = repository.findByProductId(productId)
            .orElseThrow<RuntimeException> { NotFoundException("No product found for productId: $productId") }

        val response: Product = productMapper.entityToApi(entity)
        logger.debug { "getProduct: found productId: ${response.productId}" }

        return response.copy(serviceAddress = serviceUtil.serviceAddress ?: "")
    }

    override fun createProduct(body: Product): Product? {
        return try {
            val entity: ProductEntity = productMapper.apiToEntity(body)
            val newEntity = repository.save(entity)
            logger.debug { "createProduct: entity created for productId: ${body.productId}" }
            productMapper.entityToApi(newEntity)
        } catch (dke: DuplicateKeyException) {
            throw InvalidInputException("Duplicate key, Product Id: " + body.productId)
        }
    }

    override fun deleteProduct(productId: Int) {
        logger.debug { "deleteProduct: tries to delete an entity with productId: $productId" }
        repository.findByProductId(productId).ifPresent { product -> repository.delete(product) }
    }
}