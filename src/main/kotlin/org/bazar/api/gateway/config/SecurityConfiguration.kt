package org.bazar.api.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.PredicateSpec
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import java.util.function.Function


@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration {

    @Value($$"${app.frontend.url}")
    private lateinit var frontUrl: String

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
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
            // Add this block to handle exceptions
            .exceptionHandling { handling ->
                handling.authenticationEntryPoint(
                    HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
                )
            }
            .build()
    }
}
