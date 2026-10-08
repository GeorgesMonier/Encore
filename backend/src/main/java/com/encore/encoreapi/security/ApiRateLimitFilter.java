package com.encore.encoreapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final int GENERAL_LIMIT_PER_MINUTE = 120;
    private static final int SUPPORT_LIMIT_PER_MINUTE = 10;
    private static final String SUPPORT_PATH = "/api/support/ask";

    private final ApiRateLimitService rateLimitService;

    public ApiRateLimitFilter(ApiRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/")
                || ("/api/payments/webhook".equals(request.getRequestURI())
                && "POST".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String clientAddress = request.getRemoteAddr();
        String clientKey = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress;

        ApiRateLimitService.Decision generalDecision =
                rateLimitService.tryAcquire("api:" + clientKey, GENERAL_LIMIT_PER_MINUTE);
        if (!generalDecision.allowed()) {
            reject(response, generalDecision);
            return;
        }

        if ("POST".equalsIgnoreCase(request.getMethod()) && SUPPORT_PATH.equals(request.getRequestURI())) {
            ApiRateLimitService.Decision supportDecision =
                    rateLimitService.tryAcquire("support:" + clientKey, SUPPORT_LIMIT_PER_MINUTE);
            if (!supportDecision.allowed()) {
                reject(response, supportDecision);
                return;
            }
            response.setHeader("X-RateLimit-Limit", Integer.toString(SUPPORT_LIMIT_PER_MINUTE));
            response.setHeader("X-RateLimit-Remaining", Integer.toString(supportDecision.remaining()));
        } else {
            response.setHeader("X-RateLimit-Limit", Integer.toString(GENERAL_LIMIT_PER_MINUTE));
            response.setHeader("X-RateLimit-Remaining", Integer.toString(generalDecision.remaining()));
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, ApiRateLimitService.Decision decision) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"status":429,"error":"Too Many Requests","message":"Has enviado demasiadas peticiones. Espera un minuto e inténtalo de nuevo.","timestamp":"%s"}
                """.formatted(Instant.now()));
    }
}
