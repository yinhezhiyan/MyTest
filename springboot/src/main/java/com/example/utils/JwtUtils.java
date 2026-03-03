package com.example.utils;

import com.example.entity.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtils {

    private static final String SECRET = "code2026-code2026-code2026-code2026";
    private static final long EXPIRE_MILLIS = 24 * 60 * 60 * 1000L;
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public static String createToken(Account account) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + EXPIRE_MILLIS);
        return Jwts.builder()
                .subject(account.getUsername())
                .claim("userId", account.getId())
                .claim("role", account.getRole())
                .claim("subject", account.getSubject())
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(KEY)
                .compact();
    }

    public static Claims parse(String token) {
        return Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
    }
}
