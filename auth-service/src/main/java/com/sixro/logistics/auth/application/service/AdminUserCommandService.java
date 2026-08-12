package com.sixro.logistics.auth.application.service;

import com.sixro.logistics.auth.application.command.AdminCreateUserCommand;
import com.sixro.logistics.auth.application.dto.AdminCreateUserResult;
import com.sixro.logistics.auth.domain.exception.AuthErrorCode;
import com.sixro.logistics.auth.domain.exception.AuthException;
import com.sixro.logistics.auth.domain.model.AffiliationType;
import com.sixro.logistics.auth.domain.model.UserRole;
import com.sixro.logistics.auth.domain.model.UserStatus;
import com.sixro.logistics.auth.infrastructure.client.UserServiceClient;
import com.sixro.logistics.auth.infrastructure.client.UserServiceErrorMapper;
import com.sixro.logistics.auth.infrastructure.client.request.InternalAdminCreateUserRequest;
import com.sixro.logistics.auth.infrastructure.client.response.InternalCreateUserResponse;
import com.sixro.logistics.common.core.response.CommonResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserCommandService {

    private final UserServiceClient userServiceClient;
    private final UserServiceErrorMapper userServiceErrorMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminCreateUserResult createUser(
            AdminCreateUserCommand command
    ) {
        validateRequester(
                command.requesterId(),
                command.requesterRole()
        );
        validateAffiliation(command);

        String encodedPassword =
                passwordEncoder.encode(
                        command.rawPassword()
                );

        try {
            CommonResponse<InternalCreateUserResponse> response =
                    userServiceClient.createApprovedUser(
                            command.requesterId(),
                            command.requesterRole(),
                            InternalAdminCreateUserRequest.from(
                                    command,
                                    encodedPassword
                            )
                    );

            InternalCreateUserResponse data =
                    requireData(response);

            return new AdminCreateUserResult(
                    data.userId(),
                    data.username(),
                    data.role(),
                    data.userStatus()
            );

        } catch (FeignException exception) {
            throw userServiceErrorMapper
                    .convertUserCreateException(exception);
        }
    }

    private void validateRequester(
            UUID requesterId,
            UserRole requesterRole
    ) {
        if (requesterId == null
                || requesterRole != UserRole.MASTER_ADMIN) {
            throw new AuthException(
                    AuthErrorCode.ADMIN_USER_CREATE_FORBIDDEN
            );
        }
    }

    private void validateAffiliation(
            AdminCreateUserCommand command
    ) {
        if (command.role() == null) {
            throw new AuthException(
                    AuthErrorCode.INVALID_AFFILIATION
            );
        }

        boolean valid = switch (command.role()) {
            case MASTER_ADMIN ->
                    command.affiliationId() == null
                            && command.affiliationType() == null;

            case HUB_ADMIN, DELIVERY_MANAGER ->
                    command.affiliationId() != null
                            && command.affiliationType()
                            == AffiliationType.HUB;

            case COMPANY_MANAGER ->
                    command.affiliationId() != null
                            && command.affiliationType()
                            == AffiliationType.COMPANY;
        };

        if (!valid) {
            throw new AuthException(
                    AuthErrorCode.INVALID_AFFILIATION
            );
        }
    }

    private InternalCreateUserResponse requireData(
            CommonResponse<InternalCreateUserResponse> response
    ) {
        if (response == null
                || !response.success()
                || response.data() == null) {
            throw new AuthException(
                    AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED
            );
        }

        InternalCreateUserResponse data =
                response.data();

        if (data.userId() == null
                || data.username() == null
                || data.role() == null
                || data.userStatus() != UserStatus.APPROVED) {
            throw new AuthException(
                    AuthErrorCode.USER_SERVICE_COMMUNICATION_FAILED
            );
        }

        return data;
    }
}