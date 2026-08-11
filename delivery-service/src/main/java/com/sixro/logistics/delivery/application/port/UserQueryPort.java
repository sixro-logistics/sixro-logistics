package com.sixro.logistics.delivery.application.port;

import com.sixro.logistics.delivery.application.model.UserInfo;

import java.util.Optional;
import java.util.UUID;

// app -> infra 요구 기능 정의
public interface UserQueryPort {

    Optional<UserInfo> findUser(UUID userId);
}
