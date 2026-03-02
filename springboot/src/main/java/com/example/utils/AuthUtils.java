package com.example.utils;

import com.example.common.ResultCode;
import com.example.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;

public class AuthUtils {

    private AuthUtils() {}

    public static Integer currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) throw new CustomException("未获取到当前用户信息");
        return Integer.parseInt(String.valueOf(userId));
    }

    public static String currentUserRole(HttpServletRequest request) {
        Object role = request.getAttribute("currentUserRole");
        return role == null ? "" : String.valueOf(role);
    }

    public static String currentUserSubject(HttpServletRequest request) {
        Object subject = request.getAttribute("currentUserSubject");
        return subject == null ? "" : String.valueOf(subject);
    }

    public static void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(currentUserRole(request))) {
            throw new CustomException(ResultCode.FORBIDDEN, "无权限执行该操作");
        }
    }
}
