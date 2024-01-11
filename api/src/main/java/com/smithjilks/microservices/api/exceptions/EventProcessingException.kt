package com.smithjilks.microservices.api.exceptions

class EventProcessingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)