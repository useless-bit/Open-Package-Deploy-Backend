package org.codesystem.server.configuration;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.configuration.keycloak.JwtAuthConverter;
import org.codesystem.server.filter.HeaderAuthenticationFilter;
import org.codesystem.server.repository.ServerRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    public static final String AUTHENTICATION_ROLE = "uma_authorization";
    private final JwtAuthConverter jwtAuthConverter;
    private final ServerRepository serverRepository;

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChainSwagger(HttpSecurity http) throws Exception {
        http.securityMatcher("/swagger-ui/**", "/v3/api-docs/**").authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChainMonitoring(HttpSecurity http) throws Exception {
        http.securityMatcher("/monitoring/**", "/error/**").authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain securityFilterChainAgentCommunication(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/agent/communication/**", "/api/agent/registration/**").authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(4)
    @Profile("Development")
    public SecurityFilterChain securityFilterChainWebAPIDev(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**").authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(4)
    @Profile("!Development")
    public SecurityFilterChain securityFilterChainWebAPI(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**").authorizeHttpRequests(auth -> auth.anyRequest().hasRole(AUTHENTICATION_ROLE))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthConverter)));
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(5)
    @Profile("Development")
    public SecurityFilterChain securityFilterChainAgentDownloadDev(HttpSecurity http) throws Exception {
        http.securityMatcher("/download/agent/**").authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(5)
    @Profile("!Development")
    public SecurityFilterChain securityFilterChainAgentDownload(HttpSecurity http) throws Exception {
        http.securityMatcher("/download/agent/**");
        http.addFilterBefore(new HeaderAuthenticationFilter(serverRepository), BasicAuthenticationFilter.class);
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(6)
    public SecurityFilterChain securityFilterChainDenyAll(HttpSecurity http) throws Exception {
        http.securityMatcher("**").authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
