package com.smithjilks.microservices.core.product

import com.smithjilks.microservices.api.core.product.Product
import com.smithjilks.microservices.core.product.service.ProductMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers


class MapperTests {
    private val mapper = Mappers.getMapper(ProductMapper::class.java)

    @Test
    fun mapperTests() {
        Assertions.assertNotNull(mapper)
        val api = Product(1, "n", 1f, "sa")

        val entity = mapper.apiToEntity(api)
        Assertions.assertEquals(api.productId, entity.productId)
        Assertions.assertEquals(api.productId, entity.productId)
        Assertions.assertEquals(api.name, entity.name)
        Assertions.assertEquals(api.weight, entity.weight)

        val (productId, name, weight, serviceAddress) = mapper.entityToApi(entity)
        Assertions.assertEquals(api.productId, productId)
        Assertions.assertEquals(api.productId, productId)
        Assertions.assertEquals(api.name, name)
        Assertions.assertEquals(api.weight, weight)
        Assertions.assertNull(serviceAddress)
    }
}