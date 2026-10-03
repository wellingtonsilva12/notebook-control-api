package com.notebookcontrol.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final String apiKey = System.getenv("API_KEY"); // opcional — se não definida, API fica aberta

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        boolean isHealth = "/api/health".equals(request.getRequestURI());
        boolean isPreflight = "OPTIONS".equalsIgnoreCase(request.getMethod());

        if (apiKey == null || apiKey.isBlank() || isHealth || isPreflight) {
            chain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader("x-api-key");
        if (!apiKey.equals(provided)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Chave de API inválida ou ausente.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
