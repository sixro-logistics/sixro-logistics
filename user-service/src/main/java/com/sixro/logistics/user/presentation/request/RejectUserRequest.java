package com.sixro.logistics.user.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 가입 거절 요청입니다.
 */
public record RejectUserRequest(

        @NotBlank
        @Size(max = 500)
        String rejectedReason
) {
}