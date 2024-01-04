package com.smithjilks.microservices.core.product

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container

abstract class MongoDbTestBase {
    companion object {
        @JvmStatic
        @Container
        private val database: MongoDBContainer = MongoDBContainer("mongo:6.0.4")


        init {
            database.start()
        }

        @DynamicPropertySource
        @JvmStatic
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.host") { database.host }
            registry.add("spring.data.mongodb.port") { database.getMappedPort(27017) }
            registry.add("spring.data.mongodb.database") { "test" }
        }
    }
}