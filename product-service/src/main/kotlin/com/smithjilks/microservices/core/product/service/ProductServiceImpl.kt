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
import reactor.core.publisher.Mono
import java.util.logging.Level.FINE


@RestController
class ProductServiceImpl(
    val serviceUtil: ServiceUtil,
    val repository: ProductRepository,
    val productMapper: ProductMapper,
) : ProductService {

    companion object : KLogging()

    override fun getProduct(productId: Int): Mono<Product>? {
//        if (productId < 1) {
//            throw InvalidInputException("Invalid productId: $productId")
//        }
//
//        val entity = repository.findByProductId(productId)
//            .orElseThrow<RuntimeException> { NotFoundException("No product found for productId: $productId") }
//
//        val response: Product = productMapper.entityToApi(entity)
//        logger.debug { "getProduct: found productId: ${response.productId}" }
//
//        return response.copy(serviceAddress = serviceUtil.serviceAddress ?: "")

        if (productId < 1) {
            throw InvalidInputException("Invalid productId: $productId")
        }

        logger.info("Will get product info for id = $productId")

        return repository.findByProductId(productId)
            .switchIfEmpty(Mono.error(NotFoundException("No product found for productId: $productId")))
            .log(logger.name, FINE)
            .map { productEntity -> productMapper.entityToApi(productEntity) }
            .map { productEntity -> setServiceAddress(productEntity) }
    }

    override fun createProduct(body: Product): Mono<Product>? {
//        return try {
//            val entity: ProductEntity = productMapper.apiToEntity(body)
//            val newEntity = repository.save(entity)
//            logger.debug { "createProduct: entity created for productId: ${body.productId}" }
//            productMapper.entityToApi(newEntity)
//        } catch (dke: DuplicateKeyException) {
//            throw InvalidInputException("Duplicate key, Product Id: " + body.productId)
//        }

        if (body.productId < 1) {
            throw InvalidInputException("Invalid productId: ${body.productId}")
        }

        val entity: ProductEntity = productMapper.apiToEntity(body)

        return repository.save(entity)
            .log(logger.name, FINE)
            .onErrorMap(
                DuplicateKeyException::class.java
            ) {
                InvalidInputException("Duplicate key, Product Id: ${body.productId}")
            }
            .map { productEntity ->
                productMapper.entityToApi(productEntity)
            }
    }

    override fun deleteProduct(productId: Int): Mono<Void>? {
//        logger.debug { "deleteProduct: tries to delete an entity with productId: $productId" }
//        repository.findByProductId(productId).ifPresent { product -> repository.delete(product) }

        if (productId < 1) {
            throw InvalidInputException("Invalid productId: $productId")
        }
        logger.debug { "deleteProduct: tries to delete an entity with productId: $productId" }

        return repository.findByProductId(productId)
            .log(logger.name, FINE)
            .map<Mono<Void>> { productEntity ->
                repository.delete(productEntity)
            }
            .flatMap { e: Mono<Void>? -> e }
    }

    private fun setServiceAddress(product: Product): Product {
        return product.copy(serviceAddress = serviceUtil.serviceAddress)
    }
}