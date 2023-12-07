package com.smithjilks.microservices.core.review

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ReviewServiceApplication

fun main(args: Array<String>) {
	runApplication<ReviewServiceApplication>(*args)
}
