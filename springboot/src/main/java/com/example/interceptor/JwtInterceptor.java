package com.example.interceptor;

import cn.hutool.core.util.StrUtil;
import com.example.context.UserContext;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.example.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (StrUtil.isBlank(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new CustomException("401");
        }
        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = JwtUtils.parse(token);
        } catch (Exception e) {
            throw new CustomException("401");
        }
        Account account = new Account();
        account.setId((Integer) claims.get("userId", Integer.class));
        account.setRole(claims.get("role", String.class));
        account.setSubject(claims.get("subject", String.class));
        account.setUsername(claims.getSubject());
        UserContext.set(account);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
