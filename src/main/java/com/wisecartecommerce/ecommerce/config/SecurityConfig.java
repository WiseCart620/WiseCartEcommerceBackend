package com.wisecartecommerce.ecommerce.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

import lombok.RequiredArgsConstructor;

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
                // Auth endpoints
                .requestMatchers(
                        "/auth/register", "/api/auth/register",
                        "/auth/login", "/api/auth/login",
                        "/auth/refresh", "/api/auth/refresh",
                        "/auth/forgot-password", "/api/auth/forgot-password",
                        "/auth/reset-password", "/api/auth/reset-password",
                        "/auth/verify-email", "/api/auth/verify-email",
        "/auth/social-login", "/api/auth/social-login")
                .permitAll()
                // Category public endpoints
                .requestMatchers(
                        "/categories/public/**", "/api/categories/public/**",
                        "/categories", "/api/categories",
                        "/categories/tree", "/api/categories/tree",
                        "/categories/{id}", "/api/categories/{id}",
                        "/categories/{id}/products/count",
                        "/api/categories/{id}/products/count")
                .permitAll()
                .requestMatchers(
                        "/customer/shipping/estimate/**",
                        "/api/customer/shipping/estimate/**")
                .permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/customer/orders/*/track",
                        "/api/customer/orders/*/track")
                .permitAll()
                // Product public endpoints
                .requestMatchers(
                        "/products/**", "/api/products/**")
                .permitAll()
                // Reviews — GET is public, write operations require authentication
                .requestMatchers(HttpMethod.GET,
                        "/reviews/**", "/api/reviews/**")
                .permitAll()
                // Other public endpoints
                .requestMatchers(
                        "/error",
                        "/announcements/**", "/api/announcements/**",
                        "/webhooks/maya", "/api/webhooks/maya",
                        "/files/**", "/api/files/**",
                        "/uploads/**", "/api/uploads/**",
                        "/public/**", "/api/public/**",
                        "/storefront/settings", "/api/storefront/settings",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api/payments/verify/**",
                        "/payments/verify/**",
                        "/payments/failed-reason/**",
                        "/api/payments/check-order/**",
                        "/payments/check-order/**",
                        "/payment/success",
                        "/payment/failed",
                        "/payment/cancelled")
                .permitAll()
                // Role-based access
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/customer/**", "/api/customer/**").hasAnyRole("CUSTOMER", "ADMIN")
                // Authenticated endpoints
                .requestMatchers(
                        "/auth/me", "/api/auth/me",
                        "/auth/logout", "/api/auth/logout",
                        "/files/upload/**", "/api/files/upload/**",
                        "/files/delete", "/api/files/delete")
                .authenticated()
                .anyRequest().authenticated()
        )
                .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:8080",
                "https://wisecart.ph",
                "https://www.wisecart.ph"
        ));
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
