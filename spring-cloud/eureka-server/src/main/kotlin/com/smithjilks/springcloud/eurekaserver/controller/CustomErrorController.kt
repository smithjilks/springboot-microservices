package com.smithjilks.springcloud.eurekaserver.controller

import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CustomErrorController : ErrorController {

    @RequestMapping("/error")
    fun error(): ResponseEntity<Void> {
        return ResponseEntity.notFound().build()
    }
}