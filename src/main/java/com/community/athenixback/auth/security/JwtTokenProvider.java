package com.community.athenixback.auth.security;

import com.community.athenixback.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long tokenExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    public String generateAccessToken(User user) {
        return generateToken(user, tokenExpiration);
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, refreshTokenExpiration);
    }

    private String generateToken(User user, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        byte[] keyBytes = secretKey.getBytes();
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS512;
        SecretKeySpec key = new SecretKeySpec(keyBytes, 0, keyBytes.length, signatureAlgorithm.getJcaName());

        return Jwts.builder()
            .setSubject(user.getEmail())
            .claim("userId", user.getId())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, signatureAlgorithm)
            .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object userId = claims.get("userId");

        if (userId instanceof Number number) {
            return number.longValue();
        }

        if (userId instanceof String value) {
            return Long.parseLong(value);
        }

        return null;
    }

    public boolean validateToken(String token) {
        try {
            byte[] keyBytes = secretKey.getBytes();
            SecretKeySpec key = new SecretKeySpec(keyBytes, 0, keyBytes.length, SignatureAlgorithm.HS512.getJcaName());

            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        byte[] keyBytes = secretKey.getBytes();
        SecretKeySpec key = new SecretKeySpec(keyBytes, 0, keyBytes.length, SignatureAlgorithm.HS512.getJcaName());

        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public long getExpirationTime() {
        return tokenExpiration;
    }
}
