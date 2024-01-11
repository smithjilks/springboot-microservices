package com.smithjilks.microservices.core.review

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

@SpringBootApplication
@ComponentScan("com.smithjilks.microservices")
class ReviewServiceApplication(
    @Value("\${app.threadPoolSize:10}") val threadPoolSize: Int,
    @Value("\${app.taskQueueSize:100}") val taskQueueSize: Int
) {
    companion object : KLogging()

    @Bean
    fun publishEventScheduler(): Scheduler {
        logger.info { "Creates a messagingScheduler with connectionPoolSize = $threadPoolSize" }
        return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "publish-pool")
    }
}

fun main(args: Array<String>) {
    runApplication<ReviewServiceApplication>(*args)
}
