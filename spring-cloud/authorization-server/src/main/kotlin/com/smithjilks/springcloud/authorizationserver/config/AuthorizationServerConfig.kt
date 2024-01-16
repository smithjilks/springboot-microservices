package com.smithjilks.springcloud.authorizationserver.config

import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import com.smithjilks.springcloud.authorizationserver.jose.Jwks
import mu.KLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.authorization.authentication.*
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import java.time.Duration
import java.util.*
import java.util.function.Consumer


@Configuration(proxyBeanMethods = false)
class AuthorizationServerConfig {

    companion object : KLogging()

    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val rsaKey: RSAKey = Jwks.generateRsa()
        val jwkSet = JWKSet(rsaKey)
        return JWKSource<SecurityContext> { jwkSelector: JWKSelector, _: SecurityContext? ->
            jwkSelector.select(
                jwkSet
            )
        }
    }


    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
    }


    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Throws(Exception::class)
    fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer()

        authorizationServerConfigurer.authorizationEndpoint { authorizationEndpoint ->
            authorizationEndpoint.authenticationProviders(configureAuthenticationValidator())
        }

        val endpointsMatcher = authorizationServerConfigurer.endpointsMatcher

        http
            .securityMatcher(endpointsMatcher)
            .authorizeHttpRequests { authorize -> authorize.anyRequest().authenticated() }
            .csrf { csrf -> csrf.ignoringRequestMatchers(endpointsMatcher) }
            .with(authorizationServerConfigurer, Customizer.withDefaults())

        // Enable OpenId Connect
        http.getConfigurer(OAuth2AuthorizationServerConfigurer::class.java).oidc(Customizer.withDefaults())

        http
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(LoginUrlAuthenticationEntryPoint("/login"))
            }
            .oauth2ResourceServer { oauth2ResourceServer ->
                oauth2ResourceServer
                    .jwt { jwt ->
                        jwt
                            .decoder(jwtDecoder(jwkSource()))
                    }
            }
        return http.build()
    }


    @Bean
    fun registeredClientRepository(): RegisteredClientRepository {
        val writerClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("writer")
            .clientSecret("{noop}secret-writer")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .redirectUri("https:''my.redirect.uri")
            .redirectUri("https://localhost:8443/openapi/webjars/swagger-ui/oauth2-redirect.html")
            .scope(OidcScopes.OPENID)
            .scope("product:read")
            .scope("product:write")
            .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
            .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofHours(1)).build())
            .build()

        val readerClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("reader")
            .clientSecret("{noop}secret-reader")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .redirectUri("https://my.redirect.uri")
            .redirectUri("https://localhost:8443/openapi/webjars/swagger-ui/oauth2-redirect.html")
            .scope(OidcScopes.OPENID)
            .scope("product:read")
            .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
            .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofHours(1)).build())
            .build()

        return InMemoryRegisteredClientRepository(writerClient, readerClient)
    }

    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings {
        return AuthorizationServerSettings.builder().issuer("http://auth-server:9999").build()
    }

    private fun configureAuthenticationValidator(): Consumer<List<AuthenticationProvider>> {
        return Consumer<List<AuthenticationProvider>> { authenticationProviders ->
            authenticationProviders.forEach { authenticationProvider ->
                if (authenticationProvider is OAuth2AuthorizationCodeRequestAuthenticationProvider) {
                    val authenticationValidator: Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> =
                        // Override default redirect_uri validator
                        CustomRedirectUriValidator() // Reuse default scope validator
                            .andThen(OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR)
                    (authenticationProvider)
                        .setAuthenticationValidator(authenticationValidator)
                }
            }
        }
    }

    internal class CustomRedirectUriValidator :
        Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {
        override fun accept(authenticationContext: OAuth2AuthorizationCodeRequestAuthenticationContext) {
            val authorizationCodeRequestAuthentication =
                authenticationContext.getAuthentication<OAuth2AuthorizationCodeRequestAuthenticationToken>()
            val registeredClient = authenticationContext.registeredClient
            val requestedRedirectUri = authorizationCodeRequestAuthentication.redirectUri
            logger.trace { "Will validate the redirect uri $requestedRedirectUri" }

            // Use exact string matching when comparing client redirect URIs against pre-registered URIs
            if (!registeredClient.redirectUris.contains(requestedRedirectUri)) {
                logger.trace { "Redirect uri is invalid!" }
                val error = OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST)
                throw OAuth2AuthorizationCodeRequestAuthenticationException(error, null)
            }
            logger.trace { "Redirect uri is OK!" }
        }
    }
}