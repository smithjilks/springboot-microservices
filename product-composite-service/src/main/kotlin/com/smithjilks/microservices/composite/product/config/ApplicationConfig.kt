package com.smithjilks.microservices.composite.product.config

import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class ApplicationConfig {

    @Value("\${api.common.version}")
    lateinit var apiVersion: String

    @Value("\${api.common.title}")
    lateinit var apiTitle: String

    @Value("\${api.common.description}")
    lateinit var apiDescription: String

    @Value("\${api.common.termsOfService}")
    lateinit var apiTermsOfService: String

    @Value("\${api.common.license}")
    lateinit var apiLicense: String

    @Value("\${api.common.licenseUrl}")
    lateinit var apiLicenseUrl: String

    @Value("\${api.common.externalDocDesc}")
    lateinit var apiExternalDocDesc: String

    @Value("\${api.common.externalDocUrl}")
    lateinit var apiExternalDocUrl: String

    @Value("\${api.common.contact.name}")
    lateinit var apiContactName: String

    @Value("\${api.common.contact.url}")
    lateinit var apiContactUrl: String

    @Value("\${api.common.contact.email}")
    lateinit var apiContactEmail: String

    @Bean
    fun restTemplate() = RestTemplate()


    /**
     * exposes $HOST:$PORT/swagger-ui.html
     *
     * @return the common OpenAPI documentation
     */
    @Bean
    fun getOpenApiDocumentation(): OpenAPI {
        return OpenAPI()
            .info(
                Info().title(apiTitle)
                    .description(apiDescription)
                    .version(apiVersion)
                    .contact(
                        Contact()
                            .name(apiContactName)
                            .url(apiContactUrl)
                            .email(apiContactEmail)
                    )
                    .termsOfService(apiTermsOfService)
                    .license(
                        License()
                            .name(apiLicense)
                            .url(apiLicenseUrl)
                    )
            )
            .externalDocs(
                ExternalDocumentation()
                    .description(apiExternalDocDesc)
                    .url(apiExternalDocUrl)
            )
    }
}