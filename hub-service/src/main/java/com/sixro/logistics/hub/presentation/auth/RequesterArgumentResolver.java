package com.sixro.logistics.hub.presentation.auth;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.hub.application.command.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

@Component
public class RequesterArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UserContext.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        String userIdStr = request.getHeader(HeaderConstants.USER_ID);
        String role = request.getHeader(HeaderConstants.USER_ROLE);
        String affiliationType = request.getHeader(HeaderConstants.AFFILIATION_TYPE);
        String affiliationIdStr = request.getHeader(HeaderConstants.AFFILIATION_ID);

        // 공통 필수 헤더 검증
        if (userIdStr == null || userIdStr.isBlank() || role == null || role.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        UUID userId = parseUuid(userIdStr);
        UUID affiliationId = null;

        // Role 컨텍스트 검증
        switch (role) {
            case "MASTER_ADMIN" -> {
                // MASTER_ADMIN 은 null 사용 (user-service 정책 반영)
                affiliationType = null;
                affiliationId = null;
            }
            case "HUB_ADMIN" -> {
                // HUB_ADMIN은 소속 정보 필수
                if (affiliationType == null || affiliationType.isBlank() ||
                        affiliationIdStr == null || affiliationIdStr.isBlank()) {
                    throw new BaseException(CommonErrorCode.INVALID_REQUEST);
                }
                affiliationId = parseUuid(affiliationIdStr);
            }
            default -> {
                // 정의되지 않은 역할
                throw new BaseException(CommonErrorCode.INVALID_REQUEST);
            }
        }

        return new UserContext(userId, role, affiliationType, affiliationId);
    }

    // UUID 파싱 예외 (IllegalArgumentException)
    private UUID parseUuid(String uuidStr) {
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw new BaseException(CommonErrorCode.INVALID_PARAMETER);
        }
    }
}