package com.smithjilks.springcloud.authorizationserver


import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@SpringBootTest(properties = ["eureka.client.enabled=false", "spring.cloud.config.enabled=false"])
@AutoConfigureMockMvc
class AuthorizationServerApplicationTests {

    @Autowired
    lateinit var mvc: MockMvc

    @Test
    fun contextLoads() {
    }

    @Test
    @Throws(Exception::class)
    fun requestTokenUsingClientCredentialsGrantType() {
        val base64Credentials: String = Base64.getEncoder().encodeToString("writer:secret-writer".toByteArray())
        mvc.perform(
            post("/oauth2/token")
                .param("grant_type", "client_credentials")
                .header("Authorization", "Basic $base64Credentials")
        )
            .andExpect(status().isOk())
    }

    @Test
    @Throws(Exception::class)
    fun requestOpenidConfiguration() {
        mvc.perform(get("/.well-known/openid-configuration"))
            .andExpect(status().isOk())
    }

    @Test
    @Throws(Exception::class)
    fun requestJwkSet() {
        mvc.perform(get("/oauth2/jwks"))
            .andExpect(status().isOk())
    }

//    @Test
//    @Throws(Exception::class)
//    fun healthy() {
//        mvc.perform(get("/actuator/health"))
//            .andExpect(status().isOk())
//            .andExpect(jsonPath("$.status", `is`("UP")))
//    }
}
