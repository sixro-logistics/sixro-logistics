package com.sixro.logistics.order.application.port;

import com.sixro.logistics.order.application.model.UserInfo;

import java.util.UUID;

public interface UserQueryPort {

    UserInfo getUser(UUID userId);

}
