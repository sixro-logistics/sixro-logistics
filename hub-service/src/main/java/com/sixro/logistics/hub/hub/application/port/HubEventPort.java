package com.sixro.logistics.hub.hub.application.port;

import com.sixro.logistics.hub.hub.domain.event.HubStatusChangedEvent;

public interface HubEventPort {
    void publishStatusChangedEvent(HubStatusChangedEvent event);
}