package com.smithjilks.microservices.core.review.service

import com.smithjilks.microservices.api.core.review.Review
import com.smithjilks.microservices.api.core.review.ReviewService
import com.smithjilks.microservices.util.ServiceUtil
import org.springframework.web.bind.annotation.RestController


@RestController
class ReviewServiceImpl(val serviceUtil: ServiceUtil) : ReviewService {
    override fun getReviews(productId: Int): List<Review> {
        return listOf(
            Review(productId, 1, "Author 1", "Subject 1", "Content 1", serviceUtil.serviceAddress ?: ""),
            Review(productId, 2, "Author 2", "Subject 2", "Content 2", serviceUtil.serviceAddress ?: ""),
            Review(productId, 3, "Author 3", "Subject 3", "Content 3", serviceUtil.serviceAddress ?: "")
        )
    }
}