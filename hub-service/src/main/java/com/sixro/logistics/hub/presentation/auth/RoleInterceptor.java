package com.sixro.logistics.hub.presentation.auth;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 메서드 -> 클래스 순서로 @RequireRole 확인
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }

        // 없으면 권한 확인 생략
        if (requireRole == null) {
            return true;
        }

        String userRole = request.getHeader(HeaderConstants.USER_ROLE);

        if (userRole == null || userRole.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED); // 헤더 자체가 없으면 401
        }

        boolean hasRole = Arrays.asList(requireRole.value()).contains(userRole);
        if (!hasRole) {
            throw new BaseException(CommonErrorCode.FORBIDDEN); // 권한이 안 맞으면 403
        }

        return true;
    }
}