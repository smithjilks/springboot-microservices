package com.smithjilks.microservices.util

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.UnknownHostException

@Component
class ServiceUtil {

    @Value("\${server.port}")
    private lateinit var port: String

    companion object : KLogging()

    final var serviceAddress: String? = null
        get() {
            if (field == null) {
                field = findMyHostname() + "/" + findMyIpAddress() + ":" + port
            }
            return field
        }
        private set

    private fun findMyHostname(): String {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: UnknownHostException) {
            "unknown host name"
        }
    }

    private fun findMyIpAddress(): String {
        return try {
            InetAddress.getLocalHost().hostAddress
        } catch (e: UnknownHostException) {
            "unknown IP address"
        }
    }

}