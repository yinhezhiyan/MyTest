package com.example.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import com.example.common.ResultCode;
import com.example.exception.CustomException;
import com.example.utils.JwtTokenUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        String token = authorization;
        if (StrUtil.isNotBlank(authorization) && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        if (!jwtTokenUtils.verify(token)) {
            throw new CustomException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        JWT jwt = jwtTokenUtils.parse(token);
        Object exp = jwt.getPayload("exp");
        if (exp == null || Long.parseLong(exp.toString()) < System.currentTimeMillis()) {
            throw new CustomException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        request.setAttribute("currentUserId", Integer.parseInt(jwt.getPayload("userId").toString()));
        request.setAttribute("currentUserRole", String.valueOf(jwt.getPayload("role")));
        return true;
    }
}
