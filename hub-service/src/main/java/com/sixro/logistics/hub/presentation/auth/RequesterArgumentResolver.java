package com.sixro.logistics.hub.presentation.auth;

import com.sixro.logistics.common.constant.HeaderConstants;
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
        return parameter.getParameterType().equals(Requester.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        String userIdStr = request.getHeader(HeaderConstants.USER_ID);
        String role = request.getHeader(HeaderConstants.USER_ROLE);
        String affiliationType = request.getHeader(HeaderConstants.AFFILIATION_TYPE);
        String affiliationIdStr = request.getHeader(HeaderConstants.AFFILIATION_ID);

        // Gateway를 거치지 않은 요청 방어
        UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;
        UUID affiliationId = affiliationIdStr != null ? UUID.fromString(affiliationIdStr) : null;

        return new Requester(userId, role, affiliationType, affiliationId);
    }
}