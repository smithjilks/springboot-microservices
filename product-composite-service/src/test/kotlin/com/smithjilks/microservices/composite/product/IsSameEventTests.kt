package com.smithjilks.microservices.composite.product

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.smithjilks.microservices.api.core.product.Product
import com.smithjilks.microservices.api.event.Event
import com.smithjilks.microservices.composite.product.IsSameEvent.Companion.sameEventExceptCreatedAt
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test


class IsSameEventTests {
    private val mapper = ObjectMapper()

    @Test
    @Throws(JsonProcessingException::class)
    fun testEventObjectCompare() {

        // Event #1 and #2 are the same event, but occurs as different times
        // Event #3 and #4 are different events
        val event1: Event<Int, Product> = Event(Event.Type.CREATE, 1, Product(1, "name", 1f, null))
        val event2: Event<Int, Product> = Event(Event.Type.CREATE, 1, Product(1, "name", 1f, null))
        val event3: Event<Int, Product?> = Event(Event.Type.DELETE, 1, null)
        val event4: Event<Int, Product> = Event(Event.Type.CREATE, 1, Product(2, "name", 1f, null))
        val event1Json = mapper.writeValueAsString(event1)

        assertThat(event1Json, `is`(sameEventExceptCreatedAt(event2)))
        assertThat(event1Json, not(sameEventExceptCreatedAt(event3)))
        assertThat(event1Json, not(sameEventExceptCreatedAt(event4)))
    }
}