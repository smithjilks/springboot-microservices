package com.smithjilks.microservices.composite.product

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.smithjilks.microservices.api.event.Event
import mu.KLogging
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import java.io.IOException


class IsSameEvent private constructor(private val expectedEvent: Event<*, *>?) : TypeSafeMatcher<String>() {
    companion object {
        fun sameEventExceptCreatedAt(expectedEvent: Event<*, *>): Matcher<String> {
            return IsSameEvent(expectedEvent)
        }

        val logger = KLogging().logger
    }

    private val mapper = ObjectMapper()

    override fun matchesSafely(eventAsJson: String): Boolean {
        if (expectedEvent == null) {
            return false
        }
        logger.trace { "Convert the following json string to a map: $eventAsJson" }
        val mapEvent = convertJsonStringToMap(eventAsJson)
        mapEvent.remove("eventCreatedAt")
        val mapExpectedEvent = getMapWithoutCreatedAt(expectedEvent)
        logger.trace { "Got the map: $mapEvent" }
        logger.trace { "Compare to the expected map: $mapExpectedEvent" }
        return mapper.writeValueAsString(mapEvent) == mapper.writeValueAsString(mapExpectedEvent)

        // TODO( Find out cause of failure when comparing the two maps)
        //return mapEvent == mapExpectedEvent
    }

    override fun describeTo(description: Description) {
        val expectedJson = convertObjectToJsonString(expectedEvent)
        description.appendText("expected to look like $expectedJson")
    }

    private fun getMapWithoutCreatedAt(event: Any): MutableMap<*, *> {
        val mapEvent = convertObjectToMap(event)
        mapEvent.remove("eventCreatedAt")
        return mapEvent
    }

    private fun convertObjectToMap(`object`: Any): MutableMap<*, *> {
        val node: JsonNode = mapper.convertValue(`object`, JsonNode::class.java)
        return mapper.convertValue(node, MutableMap::class.java)
    }

    private fun convertObjectToJsonString(`object`: Any?): String {
        return try {
            mapper.writeValueAsString(`object`)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    private fun convertJsonStringToMap(eventAsJson: String): MutableMap<*, *> {
        return try {
            mapper.readValue(eventAsJson, object : TypeReference<MutableMap<*, *>>() {})
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

}