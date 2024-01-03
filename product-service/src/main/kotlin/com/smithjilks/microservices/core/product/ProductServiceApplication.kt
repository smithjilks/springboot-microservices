package com.smithjilks.microservices.core.product

import com.smithjilks.microservices.core.product.persitence.ProductEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.index.IndexDefinition
import org.springframework.data.mongodb.core.index.IndexResolver
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver


@SpringBootApplication
@ComponentScan("com.smithjilks.microservices")
class ProductServiceApplication {
    @Autowired
    lateinit var mongoTemplate: MongoOperations

    @EventListener(ContextRefreshedEvent::class)
    fun initIndicesAfterStartup() {
        val mappingContext = mongoTemplate.converter.mappingContext
        val resolver: IndexResolver = MongoPersistentEntityIndexResolver(mappingContext)
        val indexOps = mongoTemplate.indexOps(ProductEntity::class.java)
        resolver.resolveIndexFor(ProductEntity::class.java).forEach { e: IndexDefinition? -> indexOps.ensureIndex(e!!) }
    }
}

fun main(args: Array<String>) {
    runApplication<ProductServiceApplication>(*args)
}
