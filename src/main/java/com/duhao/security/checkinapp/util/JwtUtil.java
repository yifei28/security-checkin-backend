package com.duhao.security.checkinapp.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration}")
    private long EXPIRATION_TIME;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT token
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateWechatToken(String openid) {
        // 24 小时和 48 小时的毫秒数
        long minMillis = 24 * 60 * 60 * 1000L;
        long maxMillis = 48 * 60 * 60 * 1000L;

        // 随机生成一个介于 [minMillis, maxMillis) 的值
        long randomMillis = minMillis + ThreadLocalRandom
                .current()
                .nextLong(maxMillis - minMillis);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + randomMillis);

        return Jwts.builder()
                .setSubject("openid:" + openid)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 token 并提取用户名
     */
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 验证 token 是否有效
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析 token 得到 Claims
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}