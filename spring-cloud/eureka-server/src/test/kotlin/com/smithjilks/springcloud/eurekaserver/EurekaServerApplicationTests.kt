package com.smithjilks.springcloud.eurekaserver


import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaServerApplicationTests {

    @Test
    fun contextLoads() {
    }

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate


    @Test
    fun catalogLoads() {
        val expectedResponseBody =
            "{\"applications\":{\"versions__delta\":\"1\",\"apps__hashcode\":\"\",\"application\":[]}}"
        val entity = testRestTemplate.getForEntity("/eureka/apps", String::class.java)
        Assertions.assertEquals(HttpStatus.OK, entity.statusCode)
        Assertions.assertEquals(expectedResponseBody, entity.body)
    }

    @Test
    fun healthy() {
        val expectedResponseBody = "{\"status\":\"UP\"}"
        val entity = testRestTemplate.getForEntity("/actuator/health", String::class.java)
        Assertions.assertEquals(HttpStatus.OK, entity.statusCode)
        Assertions.assertEquals(expectedResponseBody, entity.body)
    }

}
