package com.smithjilks.microservices.util


import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

import com.smithjilks.microservices.api.exceptions.InvalidInputException
import com.smithjilks.microservices.api.exceptions.NotFoundException
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice
internal class GlobalControllerExceptionHandler {
    companion object : KLogging()


    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(NotFoundException::class)
    @ResponseBody
    fun handleNotFoundExceptions(
        request: ServerHttpRequest, ex: NotFoundException
    ): HttpErrorInfo {
        return createHttpErrorInfo(NOT_FOUND, request, ex)
    }

    @ResponseStatus(UNPROCESSABLE_ENTITY)
    @ExceptionHandler(InvalidInputException::class)
    @ResponseBody
    fun handleInvalidInputException(
        request: ServerHttpRequest, ex: InvalidInputException
    ): HttpErrorInfo {
        return createHttpErrorInfo(UNPROCESSABLE_ENTITY, request, ex)
    }

    private fun createHttpErrorInfo(
        httpStatus: HttpStatus, request: ServerHttpRequest, ex: Exception
    ): HttpErrorInfo {
        val path: String = request.path.pathWithinApplication().value()
        val message = ex.message
        logger.debug(
            "Returning HTTP status: {} for path: {}, message: {}",
            httpStatus,
            path,
            message
        )
        return HttpErrorInfo(httpStatus = httpStatus, path = path, message = message)
    }

}