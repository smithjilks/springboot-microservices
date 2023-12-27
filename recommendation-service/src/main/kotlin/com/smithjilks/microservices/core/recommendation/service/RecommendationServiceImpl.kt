package com.smithjilks.microservices.core.recommendation.service

import com.smithjilks.microservices.api.core.recommendation.Recommendation
import com.smithjilks.microservices.api.core.recommendation.RecommendationService
import com.smithjilks.microservices.util.ServiceUtil
import org.springframework.web.bind.annotation.RestController


@RestController
class RecommendationServiceImpl(val serviceUtil: ServiceUtil) : RecommendationService {
    override fun getRecommendations(productId: Int): List<Recommendation> {
        return listOf(
            Recommendation(productId, 1, "Author 1", 1, "Content 1", serviceUtil.serviceAddress ?: ""),
            Recommendation(productId, 2, "Author 2", 2, "Content 2", serviceUtil.serviceAddress ?: ""),
            Recommendation(productId, 3, "Author 3", 3, "Content 3", serviceUtil.serviceAddress ?: ""),
        )
    }
}