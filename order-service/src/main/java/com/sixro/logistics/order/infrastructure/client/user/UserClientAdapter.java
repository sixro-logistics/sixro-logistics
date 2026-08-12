package com.sixro.logistics.order.infrastructure.client.user;

import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.order.application.model.UserInfo;
import com.sixro.logistics.order.application.port.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserClientAdapter implements UserQueryPort {

    private final UserClient userClient;

    @Override
    public UserInfo getUser(UUID userId) {

        CommonResponse<UserClientResponse> response = userClient.getUser(userId);

        return new UserInfo(
                response.data().userId(),
                response.data().role(),
                response.data().affiliationId(),
                response.data().affiliationType(),
                response.data().userStatus()
        );
    }

}
