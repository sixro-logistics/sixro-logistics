package com.sixro.logistics.auth.application.service;

import com.sixro.logistics.auth.domain.repository.RefreshTokenRepository;
import com.sixro.logistics.auth.domain.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 사용자 보안 상태 변경 시 Session과 Refresh Token을 제거하는
 * 인증 상태 무효화 서비스의 단위 테스트입니다.
 *
 * <p>Gateway가 기존 Access Token을 즉시 차단할 수 있도록
 * Session을 먼저 삭제하는지 확인하고, Redis 저장소 오류가
 * 호출자에게 전파되는지 검증합니다.</p>
 *
 * <p>실제 Redis는 사용하지 않습니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserAuthStateInvalidationServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserAuthStateInvalidationService invalidationService;

    @Test
    void 사용자_세션과_Refresh_Token을_순서대로_삭제한다() {
        UUID userId = UUID.randomUUID();

        invalidationService.invalidate(userId);

        InOrder inOrder = inOrder(sessionRepository, refreshTokenRepository);
        inOrder.verify(sessionRepository).deleteByUserId(userId);
        inOrder.verify(refreshTokenRepository).deleteByUserId(userId);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void 세션_삭제에_실패하면_예외를_전파하고_Refresh_Token은_삭제하지_않는다() {
        UUID userId = UUID.randomUUID();
        RuntimeException redisException = new RuntimeException("Redis connection failed");

        doThrow(redisException)
                .when(sessionRepository)
                .deleteByUserId(userId);

        assertThatThrownBy(() -> invalidationService.invalidate(userId))
                .isSameAs(redisException);

        verify(sessionRepository).deleteByUserId(userId);
        verify(refreshTokenRepository, never()).deleteByUserId(userId);
    }

    @Test
    void Refresh_Token_삭제에_실패하면_세션_삭제_후_예외를_전파한다() {
        UUID userId = UUID.randomUUID();
        RuntimeException redisException =
                new RuntimeException("Redis connection failed");

        doThrow(redisException)
                .when(refreshTokenRepository)
                .deleteByUserId(userId);

        assertThatThrownBy(() -> invalidationService.invalidate(userId))
                .isSameAs(redisException);

        InOrder inOrder = inOrder(
                sessionRepository,
                refreshTokenRepository
        );
        inOrder.verify(sessionRepository).deleteByUserId(userId);
        inOrder.verify(refreshTokenRepository).deleteByUserId(userId);
    }

    @Test
    void 동일한_이벤트가_반복되어도_동일한_삭제_연산을_수행한다() {
        UUID userId = UUID.randomUUID();

        invalidationService.invalidate(userId);
        invalidationService.invalidate(userId);

        verify(sessionRepository, times(2))
                .deleteByUserId(userId);
        verify(refreshTokenRepository, times(2))
                .deleteByUserId(userId);
    }
}
