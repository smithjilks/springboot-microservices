package com.smithjilks.microservices.core.recommendation.service

import com.smithjilks.microservices.api.core.recommendation.Recommendation
import com.smithjilks.microservices.core.recommendation.persistence.RecommendationEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings


@Mapper(componentModel = "spring")
interface RecommendationMapper {

    @Mappings(
        Mapping(target = "rate", source = "entity.rating"),
        Mapping(target = "serviceAddress", ignore = true)
    )
    fun entityToApi(entity: RecommendationEntity): Recommendation

    @Mappings(
        Mapping(target = "rating", source = "api.rate"),
        Mapping(target = "id", ignore = true),
        Mapping(target = "version", ignore = true)
    )
    fun apiToEntity(api: Recommendation): RecommendationEntity

    fun entityListToApiList(entity: List<RecommendationEntity>): List<Recommendation>

    fun apiListToEntityList(api: List<Recommendation?>): List<RecommendationEntity>
}