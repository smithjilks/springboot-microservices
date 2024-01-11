package com.smithjilks.microservices.api.event

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now


data class Event<K, T>(
    val eventType: Type,
    val key: K,
    val data: T,
    @get:JsonSerialize(using = ZonedDateTimeSerializer::class)
    val eventCreatedAt: ZonedDateTime = now()
) {
    enum class Type {
        CREATE,
        DELETE
    }
}


