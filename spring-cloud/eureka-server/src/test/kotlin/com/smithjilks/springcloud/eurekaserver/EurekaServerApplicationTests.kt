package com.smithjilks.springcloud.eurekaserver


import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = ["app.eureka-username=testuser", "app.eureka-password=testpassword"])
class EurekaServerApplicationTests {

    @Value("\${app.eureka-username}")
    final lateinit var username: String

    @Value("\${app.eureka-password}")
    final lateinit var password: String

    @Test
    fun contextLoads() {
    }

    @Autowired
    final lateinit var testRestTemplate: TestRestTemplate


    @Test
    fun catalogLoads() {
        val expectedResponseBody =
            "{\"applications\":{\"versions__delta\":\"1\",\"apps__hashcode\":\"\",\"application\":[]}}"
        val entity = testRestTemplate.withBasicAuth(username, password).getForEntity("/eureka/apps", String::class.java)
        Assertions.assertEquals(HttpStatus.OK, entity.statusCode)
        Assertions.assertEquals(expectedResponseBody, entity.body)
    }

    @Test
    fun healthy() {
        val expectedResponseBody = "{\"status\":\"UP\"}"
        val entity =
            testRestTemplate.withBasicAuth(username, password).getForEntity("/actuator/health", String::class.java)
        Assertions.assertEquals(HttpStatus.OK, entity.statusCode)
        Assertions.assertEquals(expectedResponseBody, entity.body)
    }

}

