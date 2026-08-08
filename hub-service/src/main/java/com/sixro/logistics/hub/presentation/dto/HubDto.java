package com.sixro.logistics.hub.presentation.dto;

import com.sixro.logistics.hub.domain.model.HubStatus;
import com.sixro.logistics.hub.domain.model.HubZone;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class HubDto {

    public record CreateRequest(
            @NotBlank String hubName,
            @NotBlank String zipcode,
            @NotBlank String roadAddress,
            String jibunAddress,
            String detailAddress,
            @NotNull Double longitude,
            @NotNull Double latitude,
            @NotNull HubZone hubZone,
            @Min(10_000) int maxCapacity
    ) {}

    public record UpdateRequest(
            @NotBlank String hubName,
            @NotBlank String zipcode,
            @NotBlank String roadAddress,
            String jibunAddress,
            String detailAddress,
            @NotNull Double longitude,
            @NotNull Double latitude,
            @NotNull HubZone hubZone,
            @Min(10_000) int maxCapacity
    ) {}

    public record StatusUpdateRequest(
            @NotNull HubStatus hubStatus
    ) {}

    public record Response(
            UUID hubId,
            String hubName,
            String zipcode,
            String roadAddress,
            String jibunAddress,
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