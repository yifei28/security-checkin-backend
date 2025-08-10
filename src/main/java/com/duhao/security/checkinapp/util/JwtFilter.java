package com.duhao.security.checkinapp.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Allow login request to pass through without token
        if (request.getRequestURI().equals("/api/login") ||
                request.getRequestURI().equals("/api/wechat-login") ||
                    request.getRequestURI().equals("/api/wechat-launch") ||
                        request.getRequestURI().equals("/api/wechat-refresh-token") ||
                            request.getRequestURI().startsWith("/api/test/") ||
                                request.getRequestURI().equals("/api/checkin/test-mini-program") ||
                                    request.getRequestURI().startsWith("/demo/")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.isTokenValid(token)) {
                String username = jwtUtil.extractUsername(token);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // ❌ Invalid or expired token — stop here and return 401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Token is invalid or expired\"}");
                return;
            }
        } else {
            // ❌ Missing Authorization header — block it too
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Authorization header is missing or malformed\"}");
            return;
        }

        // ✅ If token is valid, continue the request
        filterChain.doFilter(request, response);
    }
}
