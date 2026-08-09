package com.sixro.logistics.user.presentation.request;

import com.sixro.logistics.user.application.command.UpdateUserCommand;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * 사용자 정보 부분 수정 요청입니다.
 *
 * <p>PATCH 요청이므로 전달되지 않은 필드는 변경하지 않습니다.</p>
 */
public record UpdateUserRequest(

        @Size(max = 100)
        @Pattern(
                regexp = ".*\\S.*",
                message = "Slack ID는 공백으로만 구성할 수 없습니다."
        )
        String slackId,
        UserRole role,
        UUID affiliationId,
        AffiliationType affiliationType
) {

    public UpdateUserCommand toCommand(
            UUID targetUserId,
            UUID requesterId,
            UserRole requesterRole
    ) {
        return new UpdateUserCommand(
                targetUserId,
                requesterId,
                requesterRole,
                slackId,
                role,
                affiliationId,
                affiliationType
        );
    }
}