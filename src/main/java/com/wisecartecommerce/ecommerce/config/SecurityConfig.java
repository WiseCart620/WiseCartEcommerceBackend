package com.wisecartecommerce.ecommerce.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                // Auth endpoints (with and without /api prefix)
                                                .requestMatchers(
                                                                "/auth/register", "/api/auth/register",
                                                                "/auth/login", "/api/auth/login",
                                                                "/auth/refresh", "/api/auth/refresh",
                                                                "/auth/forgot-password", "/api/auth/forgot-password",
                                                                "/auth/reset-password", "/api/auth/reset-password",
                                                                "/auth/verify-email", "/api/auth/verify-email")
                                                .permitAll()

                                                // Category public endpoints (with and without /api prefix)
                                                .requestMatchers(
                                                                "/categories/public/**", "/api/categories/public/**",
                                                                "/categories", "/api/categories",
                                                                "/categories/tree", "/api/categories/tree",
                                                                "/categories/{id}", "/api/categories/{id}",
                                                                "/categories/{id}/products/count",
                                                                "/api/categories/{id}/products/count"

                                                ).permitAll()

                                                .requestMatchers(
                                                                "/customer/shipping/estimate/**",
                                                                "/api/customer/shipping/estimate/**")
                                                .permitAll()

                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/customer/orders/*/track",
                                                                "/api/customer/orders/*/track")
                                                .permitAll()

                                                // Product public endpoints
                                                .requestMatchers(
                                                                "/products/**", "/api/products/**")
                                                .permitAll()

                                                // Reviews public endpoints (read-only)
                                                .requestMatchers(
                                                                "/reviews/**", "/api/reviews/**")
                                                .permitAll()

                                                // Other public endpoints
                                                .requestMatchers(
                                                                "/error",
                                                                "/announcements/**", "/api/announcements/**",
                                                                "/files/serve/**", "/api/files/serve/**",
                                                                "/uploads/**", "/api/uploads/**",
                                                                "/public/**", "/api/public/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()

                                                // Role-based access
                                                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/customer/**", "/api/customer/**").hasRole("CUSTOMER")

                                                // Authenticated endpoints
                                                .requestMatchers(
                                                                "/auth/me", "/api/auth/me",
                                                                "/auth/logout", "/api/auth/logout",
                                                                "/files/upload/**", "/api/files/upload/**",
                                                                "/files/delete", "/api/files/delete")
                                                .authenticated()

                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setExposedHeaders(Arrays.asList("Content-Disposition"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}