package com.example.utils;

import com.example.entity.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private static SecretKey key;
    private static long expireMillis;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-millis:86400000}")
    private long configuredExpireMillis;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        expireMillis = configuredExpireMillis;
    }

    public static String createToken(Account account) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireMillis);
        return Jwts.builder()
                .subject(account.getUsername())
                .claim("userId", account.getId())
                .claim("role", account.getRole())
                .claim("subject", account.getSubject())
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(key)
                .compact();
    }

    public static Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
