package com.materimperium.backendtest.config;

import com.materimperium.backendtest.logging.CorrelationIdFilter;
import com.materimperium.backendtest.security.BearerTokenFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorrelationIdFilter correlationIdFilter,
            BearerTokenFilter bearerTokenFilter
    )
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/uploads/*/resultado").hasRole("CONSULTA")
                        .requestMatchers("/api/uploads/*/status").hasAnyRole("ENVIO", "CONSULTA")
                        .requestMatchers("/api/uploads").hasRole("ENVIO")
                        .anyRequest().authenticated())
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(bearerTokenFilter, CorrelationIdFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"erro\":\"Nao autenticado\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"erro\":\"Acesso negado\"}");
                        }))
                .build();
    }
}
