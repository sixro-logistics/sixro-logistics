package com.sixro.logistics.user.presentation.controller;

import com.sixro.logistics.common.constant.HeaderConstants;
import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.common.core.response.PageResponse;
import com.sixro.logistics.common.core.util.PageUtil;
import com.sixro.logistics.user.application.command.ApproveUserCommand;
import com.sixro.logistics.user.application.command.DeactivateUserCommand;
import com.sixro.logistics.user.application.command.RejectUserCommand;
import com.sixro.logistics.user.application.dto.UserResult;
import com.sixro.logistics.user.application.service.UserCommandService;
import com.sixro.logistics.user.application.service.UserQueryService;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.model.UserStatus;
import com.sixro.logistics.user.domain.repository.UserSearchCondition;
import com.sixro.logistics.user.presentation.request.RejectUserRequest;
import com.sixro.logistics.user.presentation.request.UpdateUserRequest;
import com.sixro.logistics.user.presentation.response.ApproveUserResponse;
import com.sixro.logistics.user.presentation.response.DeactivateUserResponse;
import com.sixro.logistics.user.presentation.response.MyUserResponse;
import com.sixro.logistics.user.presentation.response.RejectUserResponse;
import com.sixro.logistics.user.presentation.response.UpdateUserResponse;
import com.sixro.logistics.user.presentation.response.UserDetailResponse;
import com.sixro.logistics.user.presentation.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

/**
 * 사용자 정보 조회·수정, 가입 심사 및 비활성화 API를 제공합니다.
 *
 * <p>Access Token은 Gateway에서 검증하며, User Service는
 * Gateway가 전달한 내부 사용자 헤더를 사용합니다.</p>
 */
@Tag(name = "User", description = "사용자 정보 조회, 수정, 가입 승인·거절 및 비활성화 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt");

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 정보를 조회합니다. 비밀번호와 인증 토큰 정보는 응답하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "A008/A009/A010 - Access Token 인증 실패"),
            @ApiResponse(responseCode = "404", description = "U001 - 사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "410", description = "U002 - 비활성화된 사용자"),
            @ApiResponse(responseCode = "500", description = "C999 - 서버 내부 오류")
    })
    @GetMapping("/me")
    public CommonResponse<MyUserResponse> getMyInformation(
            @Parameter(hidden = true) @RequestHeader(HeaderConstants.USER_ID) UUID requesterId
    ) {
        UserResult result = userQueryService.getMyInformation(requesterId);

        return CommonResponse.success("내 정보 조회에 성공했습니다.", MyUserResponse.from(result));
    }

    @Operation(
            summary = "사용자 단건 조회",
            description = "MASTER_ADMIN이 특정 사용자의 상세 정보와 가입 심사 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 단건 조회 성공"),
            @ApiResponse(responseCode = "401", description = "A008/A009/A010 - Access Token 인증 실패"),
            @ApiResponse(responseCode = "403", description = "U003 - 사용자 관리 권한 없음"),
            @ApiResponse(responseCode = "404", description = "U001 - 사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "410", description = "U002 - 비활성화된 사용자"),
            @ApiResponse(responseCode = "500", description = "C999 - 서버 내부 오류")
    })
    @GetMapping("/{userId}")
    public CommonResponse<UserDetailResponse> getUser(
            @Parameter(description = "조회 대상 사용자 ID", required = true)
            @PathVariable UUID userId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole requesterRole
    ) {
        UserResult result = userQueryService.getUser(userId, requesterRole);

        return CommonResponse.success("사용자 조회에 성공했습니다.", UserDetailResponse.from(result));
    }

    @Operation(
            summary = "사용자 목록 조회 및 검색",
            description = """
                    MASTER_ADMIN이 사용자 목록을 조회합니다.
                    username, role, userStatus, affiliationType으로 검색할 수 있습니다.

                    page 기본값은 0입니다.
                    size는 10, 30, 50을 지원하며 그 외 값은 10으로 처리합니다.
                    sortBy는 createdAt, updatedAt만 허용합니다.
                    sortDirection은 asc일 때 오름차순이며 그 외 값은 내림차순으로 처리합니다.
                    삭제된 사용자는 조회 결과에서 제외됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "C003 - 지원하지 않는 정렬 필드"),
            @ApiResponse(responseCode = "401", description = "A008/A009/A010 - Access Token 인증 실패"),
            @ApiResponse(responseCode = "403", description = "U003 - 사용자 관리 권한 없음"),
            @ApiResponse(responseCode = "500", description = "C999 - 서버 내부 오류")
    })
    @GetMapping
    public CommonResponse<PageResponse<UserSummaryResponse>> searchUsers(
            @Parameter(description = "사용자명 검색어") @RequestParam(required = false) String username,
            @Parameter(description = "사용자 권한") @RequestParam(required = false) UserRole role,
            @Parameter(description = "가입 심사 상태") @RequestParam(required = false) UserStatus userStatus,
            @Parameter(description = "소속 유형") @RequestParam(required = false) AffiliationType affiliationType,
            @Parameter(description = "페이지 번호", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기: 10, 30, 50", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 필드: createdAt, updatedAt", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "정렬 방향: asc, desc", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDirection,
            @Parameter(hidden = true) @RequestHeader(HeaderConstants.USER_ROLE) UserRole requesterRole
    ) {
        validateSortField(sortBy);

        Pageable pageable = PageUtil.toPageable(page, size, sortDirection, sortBy);
        UserSearchCondition condition = new UserSearchCondition(
                username,
                role,
                userStatus,
                affiliationType
        );

        Page<UserResult> resultPage = userQueryService.searchUsers(
                condition,
                pageable,
                requesterRole
        );

        PageResponse<UserSummaryResponse> response = PageResponse.from(
                resultPage,
                UserSummaryResponse::from
        );

        return CommonResponse.success("사용자 목록 조회에 성공했습니다.", response);
    }

    @Operation(
            summary = "사용자 정보 수정",
            description = """
                    로그인한 사용자 또는 MASTER_ADMIN이 사용자 정보를 수정합니다.

                    일반 사용자는 자신의 정보만 수정할 수 있습니다.
                    PENDING 사용자는 Slack ID와 소속 정보를 수정할 수 있습니다.
                    APPROVED 사용자는 Slack ID만 수정할 수 있습니다.
                    MASTER_ADMIN은 Slack ID, 권한 및 소속 정보를 수정할 수 있습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 정보 수정 성공"),
            @ApiResponse(responseCode = "400", description = "C002 - 요청 검증 실패 / U004 - 소속 정보 오류"),
            @ApiResponse(responseCode = "401", description = "A008/A009/A010 - Access Token 인증 실패"),
            @ApiResponse(responseCode = "403", description = "U003 - 수정 권한 없음 / U005 - 승인 사용자 소속 수정 불가"),
            @ApiResponse(responseCode = "404", description = "U001 - 사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "410", description = "U002 - 비활성화된 사용자"),
            @ApiResponse(responseCode = "500", description = "C999 - 서버 내부 오류")
    })
    @PatchMapping("/{userId}")
    public CommonResponse<UpdateUserResponse> updateUser(
            @Parameter(description = "수정 대상 사용자 ID", required = true)
            @PathVariable UUID userId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole requesterRole,

            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResult result = userCommandService.updateUser(
                request.toCommand(userId, requesterId, requesterRole)
        );

        return CommonResponse.success("사용자 정보를 수정했습니다.", UpdateUserResponse.from(result));
    }

    @Operation(
            summary = "사용자 가입 승인",
            description = "MASTER_ADMIN이 PENDING 상태 사용자의 가입을 승인하고 역할과 소속 정보를 확정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가입 승인 성공"),
            @ApiResponse(responseCode = "400", description = "U004 - 유효하지 않은 역할·소속 정보"),
            @ApiResponse(responseCode = "401", description = "A008/A009/A010 - Access Token 인증 실패"),
            @ApiResponse(responseCode = "403", description = "U003 - 가입 승인 권한 없음"),
            @ApiResponse(responseCode = "404", description = "U001 - 사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "U006 - 가입 승인 불가능한 상태"),
            @ApiResponse(responseCode = "410", description = "U002 - 비활성화된 사용자"),
            @ApiResponse(responseCode = "500", description = "C999 - 서버 내부 오류")
    })
    @PostMapping("/{userId}/approve")
    public CommonResponse<ApproveUserResponse> approveUser(
            @Parameter(description = "승인 대상 사용자 ID", required = true)
            @PathVariable UUID userId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.USER_ID) UUID reviewerId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole requesterRole
    ) {
        UserResult result = userCommandService.approveUser(
                new ApproveUserCommand(userId, reviewerId),
                requesterRole
        );

        return CommonResponse.success("가입을 승인했습니다.", ApproveUserResponse.from(result));
    }

    @Operation(
            summary = "사용자 가입 거절",
            description = "MASTER_ADMIN이 PENDING 상태 사용자의 가입을 거절하고 거절 사유를 기록합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가입 거절 성공"),
            @ApiResponse(responseCode = "400", description = "C002 - 거절 사유 검증 실패"),
            @ApiResponse(responseCode = "401", description = "A008/A009/A010 - Access Token 인증 실패"),
            @ApiResponse(responseCode = "403", description = "U003 - 가입 거절 권한 없음"),
            @ApiResponse(responseCode = "404", description = "U001 - 사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "U007 - 가입 거절 불가능한 상태"),
            @ApiResponse(responseCode = "410", description = "U002 - 비활성화된 사용자"),
            @ApiResponse(responseCode = "500", description = "C999 - 서버 내부 오류")
    })
    @PostMapping("/{userId}/reject")
    public CommonResponse<RejectUserResponse> rejectUser(
            @Parameter(description = "거절 대상 사용자 ID", required = true)
            @PathVariable UUID userId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.USER_ID) UUID reviewerId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole requesterRole,

            @Valid @RequestBody RejectUserRequest request
    ) {
        UserResult result = userCommandService.rejectUser(
                new RejectUserCommand(userId, reviewerId, request.rejectedReason()),
                requesterRole
        );

        return CommonResponse.success("가입을 거절했습니다.", RejectUserResponse.from(result));
    }

    @Operation(
            summary = "사용자 비활성화",
            description = """
                    사용자를 Soft Delete 방식으로 비활성화합니다.
                    isDeleted를 true로 변경하고 deletedAt, deletedBy를 기록합니다.
                    이미 비활성화된 사용자는 다시 비활성화할 수 없습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 비활성화 성공"),
            @ApiResponse(responseCode = "401", description = "A008/A009/A010 - Access Token 인증 실패"),
            @ApiResponse(responseCode = "403", description = "U003 - 권한 없음 / U008 - 자기 계정 비활성화 제한"),
            @ApiResponse(responseCode = "404", description = "U001 - 사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "U009 - 이미 비활성화된 사용자"),
            @ApiResponse(responseCode = "500", description = "C999 - 서버 내부 오류")
    })
    @DeleteMapping("/{userId}")
    public CommonResponse<DeactivateUserResponse> deactivateUser(
            @Parameter(description = "비활성화 대상 사용자 ID", required = true)
            @PathVariable UUID userId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,

            @Parameter(hidden = true)
            @RequestHeader(HeaderConstants.USER_ROLE) UserRole requesterRole
    ) {
        UserResult result = userCommandService.deactivateUser(
                new DeactivateUserCommand(userId, requesterId, requesterRole)
        );

        return CommonResponse.success(
                "사용자를 비활성화했습니다.",
                DeactivateUserResponse.from(result)
        );
    }

    /**
     * User Service에서 허용하는 정렬 필드인지 검증합니다.
     */
    private void validateSortField(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BaseException(CommonErrorCode.INVALID_PARAMETER);
        }
    }
}