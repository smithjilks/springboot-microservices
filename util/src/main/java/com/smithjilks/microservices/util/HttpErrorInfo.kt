package com.smithjilks.microservices.util

import org.springframework.http.HttpStatus
import java.time.ZonedDateTime


class HttpErrorInfo(
    val timestamp: ZonedDateTime = ZonedDateTime.now(),
    val path: String,
    private val httpStatus: HttpStatus,
    val message: String?,
) {
    val status: Int
        get() = httpStatus.value()
    val error: String
        get() = httpStatus.reasonPhrase
}