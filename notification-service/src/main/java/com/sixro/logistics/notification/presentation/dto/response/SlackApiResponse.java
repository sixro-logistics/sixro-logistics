package com.sixro.logistics.notification.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SlackApiResponse {

    private boolean ok;
    private String error;
}
