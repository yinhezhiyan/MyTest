package com.example.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenUtils {

    @Value("${jwt.secret:replace-with-env-secret}")
    private String secret;

    @Value("${jwt.expire-hours:24}")
    private Integer expireHours;

    public String generateToken(Integer userId, String username, String role) {
        Date now = new Date();
        Date expireAt = DateUtil.offsetHour(now, expireHours);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("role", role);
        payload.put("iat", now.getTime());
        payload.put("exp", expireAt.getTime());
        return JWTUtil.createToken(payload, secret.getBytes(StandardCharsets.UTF_8));
    }

    public JWT parse(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        return JWTUtil.parseToken(token);
    }

    public boolean verify(String token) {
        return StrUtil.isNotBlank(token) && JWTUtil.verify(token, secret.getBytes(StandardCharsets.UTF_8));
    }
}
