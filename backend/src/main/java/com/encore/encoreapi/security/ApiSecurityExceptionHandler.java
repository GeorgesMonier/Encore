package com.encore.encoreapi.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class ApiSecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiSecurityExceptionHandler.class);
    private final ObjectMapper objectMapper;

    public ApiSecurityExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        log.warn("Authentication required for {} {}", request.getMethod(), request.getRequestURI());
        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                "Inicia sesión para continuar.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        log.warn("Access denied for {} {} ({})", request.getMethod(), request.getRequestURI(),
                exception.getClass().getSimpleName());
        writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                "La solicitud no está autorizada o su protección CSRF no es válida.");
    }

    private void writeError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "status", status,
                "error", error,
                "message", message,
                "timestamp", Instant.now().toString()
        ));
    }
}
