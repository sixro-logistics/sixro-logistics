package com.sixro.logistics.auth.presentation.controller;

import com.sixro.logistics.auth.application.dto.SignUpResult;
import com.sixro.logistics.auth.application.dto.TokenResult;
import com.sixro.logistics.auth.application.service.AuthCommandService;
import com.sixro.logistics.auth.domain.exception.AuthErrorCode;
import com.sixro.logistics.auth.domain.exception.AuthException;
import com.sixro.logistics.auth.presentation.request.LoginRequest;
import com.sixro.logistics.auth.presentation.request.LogoutRequest;
import com.sixro.logistics.auth.presentation.request.ReissueTokenRequest;
import com.sixro.logistics.auth.presentation.request.SignUpRequest;
import com.sixro.logistics.auth.presentation.response.SignUpResponse;
import com.sixro.logistics.auth.presentation.response.TokenResponse;
import com.sixro.logistics.common.core.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입, 로그인, 토큰 재발급 및 로그아웃 API를 제공합니다.
 *
 * <p>요청 형식 검증과 Command 변환만 담당하며,
 * 실제 인증 정책과 토큰 처리는 {@link AuthCommandService}에 위임합니다.</p>
 */
@Tag(
        name = "Auth",
        description = "회원가입, 로그인, 토큰 재발급 및 로그아웃 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthCommandService authCommandService;

    @Operation(
            summary = "회원가입",
            description = """
                    새로운 사용자의 가입을 요청합니다.
                    비밀번호는 Auth Service에서 암호화한 후 User Service에 전달합니다.
                    
                    MASTER_ADMIN 권한은 일반 회원가입으로 생성할 수 없습니다.
                    HUB_ADMIN은 HUB 소속, COMPANY_MANAGER는 COMPANY 소속이어야 하며,
                    DELIVERY_MANAGER는 소속 유형과 소속 ID가 필요합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 요청 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "C002 - 요청 검증 실패 / A012 - 권한과 소속 정보 불일치 / A015 - 잘못된 회원가입 요청"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "A011 - MASTER_ADMIN 회원가입 불가"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "A014 - 사용자명 또는 Slack ID 중복"
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "A013 - User Service 통신 실패"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "C999 - 서버 내부 오류"
            )
    })
    @PostMapping("/sign-up")
    public CommonResponse<SignUpResponse> signUp(
            @Valid @RequestBody SignUpRequest request
    ) {
        SignUpResult result =
                authCommandService.signUp(request.toCommand());

        return CommonResponse.created(
                "회원가입 요청이 완료되었습니다.",
                SignUpResponse.from(result)
        );
    }

    @Operation(
            summary = "로그인",
            description = """
                    사용자명과 비밀번호를 검증하고
                    Access Token과 Refresh Token을 발급합니다.
                    
                    승인된 활성 사용자만 로그인할 수 있으며,
                    발급한 Refresh Token은 해시값으로 Redis에 저장합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "C002 - 요청 검증 실패"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "A001 - 아이디 또는 비밀번호 불일치"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "A008 - 승인되지 않은 사용자 / A009 - 가입이 거절된 사용자"
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "A010 - 비활성화된 사용자"
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "A013 - User Service 통신 실패"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "C999 - 서버 내부 오류"
            )
    })
    @PostMapping("/login")
    public CommonResponse<TokenResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResult result =
                authCommandService.login(request.toCommand());

        return CommonResponse.success(
                "로그인에 성공했습니다.",
                TokenResponse.from(result)
        );
    }

    @Operation(
            summary = "토큰 재발급",
            description = """
                    유효한 Refresh Token을 사용하여
                    새로운 Access Token과 Refresh Token을 발급합니다.
                    
                    Refresh Token의 JWT 유효성을 검증하고,
                    Redis에 저장된 Refresh Token 해시값과 일치하는지 확인합니다.
                    
                    재발급이 완료되면 기존 Refresh Token은 사용할 수 없습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "C002 - 요청 검증 실패"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "A004 - 유효하지 않은 Refresh Token / A005 - 만료된 Refresh Token / A006 - 저장된 Refresh Token 없음"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "C999 - 서버 내부 오류"
            )
    })
    @PostMapping("/reissue")
    public CommonResponse<TokenResponse> reissue(
            @Valid @RequestBody ReissueTokenRequest request
    ) {
        TokenResult result =
                authCommandService.reissue(request.toCommand());

        return CommonResponse.success(
                "토큰 재발급에 성공했습니다.",
                TokenResponse.from(result)
        );
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    Redis에 저장된 Refresh Token을 삭제하고,
                    Access Token을 남은 유효 시간 동안 블랙리스트에 등록합니다.
                    
                    Access Token과 Refresh Token의 사용자 및 권한 정보가
                    일치하는 경우에만 로그아웃을 처리합니다.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "C002 - 요청 검증 실패"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = """
                            A002 - 유효하지 않은 Access Token /
                            A003 - 만료된 Access Token /
                            A004 - 유효하지 않은 Refresh Token /
                            A005 - 만료된 Refresh Token /
                            A006 - 저장된 Refresh Token 없음 /
                            A016 - Access Token과 Refresh Token의 사용자 또는 권한 불일치
                            """
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "C999 - 서버 내부 오류"
            )
    })
    @PostMapping("/logout")
    public CommonResponse<Void> logout(
            @Parameter(
                    description = "Bearer 형식의 Access Token",
                    required = true,
                    example = "Bearer eyJhbGciOiJSUzI1NiJ9..."
            )
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorizationHeader,

            @Valid @RequestBody LogoutRequest request
    ) {
        String accessToken =
                extractBearerToken(authorizationHeader);

        authCommandService.logout(
                request.toCommand(accessToken)
        );

        return CommonResponse.success(
                "로그아웃에 성공했습니다."
        );
    }

    /**
     * Authorization 헤더에서 Bearer 접두사를 제거하고 Access Token을 추출합니다.
     *
     * @throws AuthException 헤더가 Bearer 형식이 아니거나 토큰이 비어 있는 경우
     */
    private String extractBearerToken(
            String authorizationHeader
    ) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthException(
                    AuthErrorCode.INVALID_ACCESS_TOKEN
            );
        }

        String accessToken = authorizationHeader
                .substring(BEARER_PREFIX.length())
                .trim();

        if (accessToken.isBlank()) {
            throw new AuthException(
                    AuthErrorCode.INVALID_ACCESS_TOKEN
            );
        }

        return accessToken;
    }
}