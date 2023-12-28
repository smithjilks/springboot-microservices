package com.smithjilks.microservices.core.product.service

import com.smithjilks.microservices.api.core.product.Product
import com.smithjilks.microservices.api.core.product.ProductService
import com.smithjilks.microservices.util.ServiceUtil
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductServiceImpl(val serviceUtil: ServiceUtil) : ProductService {
    override fun getProduct(productId: Int): Product? {
        return Product(
            productId, "name-$productId", 123F,
            serviceUtil.serviceAddress ?: ""
        )
    }
}