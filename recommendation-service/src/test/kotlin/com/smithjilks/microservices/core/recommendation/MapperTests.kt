package com.smithjilks.microservices.core.recommendation

import com.smithjilks.microservices.api.core.recommendation.Recommendation
import com.smithjilks.microservices.core.recommendation.service.RecommendationMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers


class MapperTests {

    private val mapper = Mappers.getMapper(RecommendationMapper::class.java)

    @Test
    fun mapperTests() {
        Assertions.assertNotNull(mapper)
        val api = Recommendation(1, 2, "a", 4, "C", "adr")
        val entity = mapper.apiToEntity(api)
        Assertions.assertEquals(api.productId, entity.productId)
        Assertions.assertEquals(api.recommendationId, entity.recommendationId)
        Assertions.assertEquals(api.author, entity.author)
        Assertions.assertEquals(api.rate, entity.rating)
        Assertions.assertEquals(api.content, entity.content)

        val (productId, recommendationId, author, rate, content, serviceAddress) = mapper.entityToApi(entity)
        Assertions.assertEquals(api.productId, productId)
        Assertions.assertEquals(api.recommendationId, recommendationId)
        Assertions.assertEquals(api.author, author)
        Assertions.assertEquals(api.rate, rate)
        Assertions.assertEquals(api.content, content)
        Assertions.assertNull(serviceAddress)
    }

    @Test
    fun mapperListTests() {
        Assertions.assertNotNull(mapper)
        val api = Recommendation(1, 2, "a", 4, "C", "adr")
        val apiList: List<Recommendation?> = listOf(api)
        val entityList = mapper.apiListToEntityList(apiList)
        Assertions.assertEquals(apiList.size, entityList.size)

        val (_, _, productId, recommendationId, author, rating, content) = entityList[0]
        Assertions.assertEquals(api.productId, productId)
        Assertions.assertEquals(api.recommendationId, recommendationId)
        Assertions.assertEquals(api.author, author)
        Assertions.assertEquals(api.rate, rating)
        Assertions.assertEquals(api.content, content)

        val api2List = mapper.entityListToApiList(entityList)
        Assertions.assertEquals(apiList.size, api2List.size)
        val (productId1, recommendationId1, author1, rate, content1, serviceAddress) = api2List[0]
        Assertions.assertEquals(api.productId, productId1)
        Assertions.assertEquals(api.recommendationId, recommendationId1)
        Assertions.assertEquals(api.author, author1)
        Assertions.assertEquals(api.rate, rate)
        Assertions.assertEquals(api.content, content1)
        Assertions.assertNull(serviceAddress)
    }
}

