package dev.jacid.hrApplication.infrastructure.config;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import dev.jacid.hrApplication.infrastructure.security.KeycloakRealmRoleConverter;
import dev.jacid.hrApplication.infrastructure.web.RequestIdFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Stateless JWT API: everything requires a valid Keycloak token except the API docs,
     * {@code /public/**} and the health probe. Role checks live on the controllers ({@code @PreAuthorize}).
     * CORS is applied before authentication, so browser preflight requests from the frontend succeed
     * and error responses (401/403) still carry the CORS headers the browser needs to read them.
     * <p>
     * The Prometheus scrape endpoint is only public when it is served on a separate management port
     * ({@code management.server.port}, e.g. 8081 in the container profile), which is meant to be reachable only from
     * the internal network. On the application port it needs a token like every other actuator endpoint.
     */
    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, Environment environment) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // Bearer tokens only: never create an HTTP session (no JSESSIONID cookie)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(prometheusOnManagementPort(environment)).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /** Matches the Prometheus scrape endpoint on the management port; matches nothing without a separate port. */
    static RequestMatcher prometheusOnManagementPort(Environment environment) {
        Integer managementPort = environment.getProperty("management.server.port", Integer.class);
        int serverPort = environment.getProperty("server.port", Integer.class, 8080);
        if (managementPort == null || managementPort <= 0 || managementPort == serverPort) {
            return request -> false;
        }
        String path = environment.getProperty("management.endpoints.web.base-path", "/actuator") + "/prometheus";
        return request -> request.getLocalPort() == managementPort
                && path.equals(request.getRequestURI().substring(request.getContextPath().length()));
    }

    /**
     * Origins allowed to call the API from a browser (the HRWebApp-UI single-page app). Configured with
     * {@code app.cors.allowed-origins}, a comma-separated list; defaults to the Vite dev server.
     * Authentication uses bearer tokens, not cookies, so credentials are not allowed.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(allowedOrigins.stream().map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", RequestIdFilter.HEADER));
        cors.setExposedHeaders(List.of(RequestIdFilter.HEADER, "Location"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    /**
     * The H2 web console only exists with the {@code h2} profile. It is a separate servlet with its
     * own login, renders inside frames and posts forms, so it gets its own filter chain.
     */
    @Configuration(proxyBeanMethods = false)
    @Profile("h2")
    @ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
    static class H2ConsoleSecurityConfig {

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher(PathRequest.toH2Console())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable)
                    .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
            return http.build();
        }
    }
}
