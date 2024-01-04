package com.smithjilks.microservices.core.review

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer


abstract class PostgreSqlTestBase {
    companion object {

        private val database: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16.1-alpine3.19").withStartupTimeoutSeconds(300)
                .withDatabaseName("reviews-db").withUsername("postgres").withPassword("postgres")

        init {
            database.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { database.jdbcUrl }
            registry.add("spring.datasource.username") { database.username }
            registry.add("spring.datasource.password") { database.password }
        }
    }
}