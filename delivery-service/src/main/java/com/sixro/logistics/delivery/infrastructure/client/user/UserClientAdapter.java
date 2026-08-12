package com.sixro.logistics.delivery.infrastructure.client.user;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.exception.ErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.delivery.application.model.UserInfo;
import com.sixro.logistics.delivery.application.port.UserQueryPort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserClientAdapter implements UserQueryPort {

    private final UserClient userClient;

    @Override
    public Optional<UserInfo> findUser(UUID userId) {
        try {
            CommonResponse<UserClientResponse> response = userClient.getDeliveryInfo(userId);

            if (response == null || !response.success() || response.data() == null) {
                throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR, new IllegalStateException("user-service가 유효하지 않은 응답을 반환했습니다."));
            }

            UserClientResponse data = response.data();

            return Optional.of(
                    new UserInfo(data.userId(), data.username(), data.role(), data.userStatus(), data.slackId(), data.affiliationId(), data.affiliationType())
            );
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }
}
