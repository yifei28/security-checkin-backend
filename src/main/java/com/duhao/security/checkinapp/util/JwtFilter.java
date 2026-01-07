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

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Allow login request to pass through without token
        if (request.getRequestURI().equals("/api/login") ||
                request.getRequestURI().equals("/api/health") ||
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
            logger.debug("JwtFilter: 收到请求 URI={}, token前20字符={}",
                    request.getRequestURI(),
                    token.length() > 20 ? token.substring(0, 20) + "..." : token);

            if (jwtUtil.isTokenValid(token)) {
                String subject = jwtUtil.extractUsername(token);

                // 根据 subject 前缀区分角色
                // 微信登录: subject = "openid:xxx" → ROLE_GUARD
                // 管理员登录: subject = username → ROLE_ADMIN
                String role = subject.startsWith("openid:") ? "ROLE_GUARD" : "ROLE_ADMIN";

                logger.info("JwtFilter: 认证成功 URI={}, subject={}, role={}",
                        request.getRequestURI(),
                        subject.length() > 20 ? subject.substring(0, 20) + "..." : subject,
                        role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                subject,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                logger.warn("JwtFilter: Token无效或已过期 URI={}", request.getRequestURI());
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
