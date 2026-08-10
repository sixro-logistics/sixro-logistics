package com.sixro.logistics.hub.hub.application.command;

import com.sixro.logistics.hub.hub.domain.model.HubStatus;
import com.sixro.logistics.hub.hub.domain.model.HubZone;

public class HubCommand {

    public record Create(
            String hubName,
            String zipcode,
            String roadAddress,
            String jibunAddress,
            String detailAddress,
            Double longitude,
            Double latitude,
            HubZone hubZone,
            int maxCapacity
    ) {}

    public record Update(
            String hubName,
            String zipcode,
            String roadAddress,
            String jibunAddress,
            String detailAddress,
            Double longitude,
            Double latitude,
            HubZone hubZone,
            int maxCapacity
    ) {}

    public record ChangeStatus(
            HubStatus hubStatus
    ) {}
}