package com.sixro.logistics.hub.presentation.dto;

import com.sixro.logistics.hub.domain.model.HubStatus;
import com.sixro.logistics.hub.domain.model.HubZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public class HubDto {

    public record CreateRequest(
            @NotBlank String hubName,
            @NotBlank String zipcode,
            @NotBlank String address,
            String detailAddress,
            @NotNull Double longitude,
            @NotNull Double latitude,
            @NotNull HubZone hubZone,
            @Positive int maxCapacity
    ) {}

    public record UpdateRequest(
            @NotBlank String hubName,
            @NotBlank String zipcode,
            @NotBlank String address,
            String detailAddress,
            @NotNull Double longitude,
            @NotNull Double latitude,
            @NotNull HubZone hubZone,
            @Positive int maxCapacity
    ) {}

    public record StatusUpdateRequest(
            @NotNull HubStatus hubStatus
    ) {}

    public record Response(
            UUID hubId,
            String hubName,
            String zipcode,
            String address,
            String detailAddress,
            Double longitude,
            Double latitude,
            HubZone hubZone,
            int maxCapacity,
            HubStatus hubStatus
    ) {}

    public record NearestResponse(
            UUID hubId,
            String hubName,
            String hubStatus,
            Double distanceInMeters
    ) {}
}