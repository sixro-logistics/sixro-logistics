package com.sixro.logistics.inventory.application.port;

import com.sixro.logistics.inventory.application.model.HubInfo;

import java.util.UUID;

public interface HubQueryPort {

    HubInfo getHub(UUID hubId);

}
