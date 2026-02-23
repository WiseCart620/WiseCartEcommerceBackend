package com.wisecartecommerce.ecommerce.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Get request URI for logging and public endpoint check
        String requestURI = request.getRequestURI();
        log.debug("Processing request: {} {}", request.getMethod(), requestURI);
        
        // Skip JWT validation for public endpoints FIRST
        if (isPublicEndpoint(requestURI)) {
            log.debug("Public endpoint, skipping authentication: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        final String authHeader = request.getHeader("Authorization");
        
        // If no Authorization header or doesn't start with Bearer, skip
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Authorization header or invalid format");
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Extract JWT token
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);
            
            // If email is valid and user is not already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Load user details
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("User authenticated successfully: {}", userEmail);
                } else {
                    log.warn("Invalid token for user: {}", userEmail);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            // Don't throw exception, just continue without authentication
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Check if the request URI is a public endpoint that doesn't require authentication
     */
    private boolean isPublicEndpoint(String uri) {
        // Remove /api prefix if present for checking against backend endpoints
        String normalizedUri = uri;
        if (uri.startsWith("/api/")) {
            normalizedUri = uri.substring(4); // Remove "/api" prefix
        }
        
        // Check both original and normalized URIs
        return checkPublicPatterns(uri) || checkPublicPatterns(normalizedUri);
    }
    
    private boolean checkPublicPatterns(String uri) {
        // Auth endpoints
        if (uri.equals("/auth/register") || uri.equals("/api/auth/register") ||
            uri.equals("/auth/login") || uri.equals("/api/auth/login") ||
            uri.equals("/auth/refresh") || uri.equals("/api/auth/refresh") ||
            uri.equals("/auth/forgot-password") || uri.equals("/api/auth/forgot-password") ||
            uri.equals("/auth/reset-password") || uri.equals("/api/auth/reset-password") ||
            uri.equals("/auth/verify-email") || uri.equals("/api/auth/verify-email")) {
            return true;
        }
        
        // Category endpoints - PUBLIC
        if (uri.startsWith("/categories/public") || uri.startsWith("/api/categories/public") ||
            uri.equals("/categories") || uri.equals("/api/categories") ||  // GET all categories is public
            uri.startsWith("/categories/tree") || uri.startsWith("/api/categories/tree") ||
            uri.matches("/categories/\\d+") || uri.matches("/api/categories/\\d+") ||  // GET by ID
            uri.matches("/categories/\\d+/products/count") || uri.matches("/api/categories/\\d+/products/count")) {
            return true;
        }
        
        // Product endpoints - PUBLIC
        if (uri.startsWith("/products") || uri.startsWith("/api/products")) {
            // Exclude admin endpoints
            if (!uri.contains("/admin") && !uri.contains("/admin/")) {
                return true;
            }
        }
        
        // Review endpoints - PUBLIC for getting reviews
        if (uri.startsWith("/reviews/product/") || uri.startsWith("/api/reviews/product/")) {
            return true;
        }
        
        // File serving endpoints
        if (uri.startsWith("/files/serve/") || uri.startsWith("/api/files/serve/") ||
            uri.startsWith("/uploads/") || uri.startsWith("/api/uploads/")) {
            return true;
        }
        
        // Swagger endpoints
        if (uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui") ||
            uri.equals("/swagger-ui.html") || uri.equals("/favicon.ico") ||
            uri.equals("/error")) {
            return true;
        }
        
        return false;
    }
}