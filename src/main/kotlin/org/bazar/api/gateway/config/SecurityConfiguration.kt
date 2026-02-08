package org.bazar.api.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.security.oauth2.client.autoconfigure.reactive.ReactiveOAuth2ClientAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult.match
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult.notMatch


@Configuration
@EnableWebFluxSecurity
@Profile("!local && !test")
class SecurityConfiguration(
    @Value($$"${management.server.port}") private val managementPort: Int,
) {

    @Value($$"${app.frontend.url}")
    private lateinit var frontUrl: String


    @Bean
    @Order(2)
    fun mainSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges.anyExchange().authenticated()
            }
            .oauth2Client { }
            .oauth2Login {
                it.authenticationSuccessHandler(
                    RedirectServerAuthenticationSuccessHandler(frontUrl)
                )
            }
            .exceptionHandling { handling ->
                handling.authenticationEntryPoint(
                    HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
                )
            }
            .build()
    }

    @Bean
    @Order(1)
    fun publicSpringSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .securityMatcher { exchange ->
                val localPort = exchange.request.localAddress?.port ?: 0
                if (localPort == managementPort) {
                    match()
                } else {
                    notMatch()
                }
            }
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges.anyExchange().permitAll()
            }
            .build()
    }
}
