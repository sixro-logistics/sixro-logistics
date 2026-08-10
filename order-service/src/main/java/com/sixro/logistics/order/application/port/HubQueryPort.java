package com.sixro.logistics.order.application.port;

import com.sixro.logistics.order.application.model.HubInfo;

import java.util.UUID;

public interface HubQueryPort {

    HubInfo getHub(UUID hubId);

}
