package com.lsoftware.inventoco.infrastructure.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Every API call belongs to a signed-in user: there is no anonymous data. Each request is scoped
 * to the token's subject by the use cases (see CurrentUser), so there are no role rules here.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String MCP_SESSION_ID = "Mcp-Session-Id";
    private static final String MCP_PROTOCOL_VERSION = "Mcp-Protocol-Version";

    @Value("${inventoco.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${inventoco.security.issuer}")
    private String issuer;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        .anyRequest().authenticated()
                )
                // RFC 9728: /.well-known/oauth-protected-resource[/<path>] and the resource_metadata
                // of every 401 come from Spring Security; MCP clients find authco there
                .oauth2ResourceServer(rs -> rs
                        .jwt(Customizer.withDefaults())
                        .protectedResourceMetadata(m -> m.protectedResourceMetadataCustomizer(meta -> meta
                                .authorizationServer(issuer)
                                .scope("openid").scope("profile").scope("email")
                                .resourceName("Inventoco")
                                .tlsClientCertificateBoundAccessTokens(false))))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::strip).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", MCP_SESSION_ID, MCP_PROTOCOL_VERSION));
        cfg.setExposedHeaders(List.of(HttpHeaders.WWW_AUTHENTICATE, MCP_SESSION_ID));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
