package com.smithjilks.microservices.api.exceptions


class BadRequestException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
