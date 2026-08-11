package com.sixro.logistics.user.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.user.domain.entity.User;
import com.sixro.logistics.user.domain.exception.UserErrorCode;
import com.sixro.logistics.user.domain.model.AffiliationType;
import com.sixro.logistics.user.domain.model.UserRole;
import com.sixro.logistics.user.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 사용자 조회 결과와 Soft Delete 접근 차단 정책을 검증하는
 * UserReader 단위 테스트입니다.
 *
 * <p>실제 DB를 사용하지 않고 UserRepository를 Mock으로 대체하여
 * 사용자 없음, 활성 사용자 및 삭제 상태를 구분하는지 확인합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserReaderTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID HUB_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserReader userReader;

    @Test
    @DisplayName("사용자 ID로 존재하는 사용자를 조회한다")
    void getById_existingUser_returnsUser() {
        User user = user();
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        User result = userReader.getById(USER_ID);

        assertThat(result).isSameAs(user);
        verify(userRepository).findById(USER_ID);
    }

    @Test
    @DisplayName("사용자 ID가 존재하지 않으면 USER_NOT_FOUND 예외가 발생한다")
    void getById_missingUser_throwsNotFound() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        BaseException exception = assertThrows(
                BaseException.class,
                () -> userReader.getById(USER_ID)
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("활성 사용자는 접근 가능한 사용자로 반환한다")
    void getAccessibleUser_activeUser_returnsUser() {
        User user = user();
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        User result = userReader.getAccessibleUser(USER_ID);

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("isDeleted가 true인 사용자는 접근을 차단한다")
    void getAccessibleUser_deletedFlag_throwsDeactivated() {
        User user = user();
        user.deactivate(UUID.randomUUID());
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        assertDeactivated(user);
    }

    @Test
    @DisplayName("deletedAt이 남아 있는 비정상 상태도 접근을 차단한다")
    void getAccessibleUser_deletedAtOnly_throwsDeactivated() {
        User user = user();
        ReflectionTestUtils.setField(
                user,
                "deletedAt",
                LocalDateTime.now()
        );
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        assertDeactivated(user);
    }

    @Test
    @DisplayName("deletedBy가 남아 있는 비정상 상태도 접근을 차단한다")
    void getAccessibleUser_deletedByOnly_throwsDeactivated() {
        User user = user();
        ReflectionTestUtils.setField(
                user,
                "deletedBy",
                UUID.randomUUID()
        );
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        assertDeactivated(user);
    }

    @Test
    @DisplayName("활성 username으로 사용자를 조회한다")
    void getActiveByUsername_existingUser_returnsUser() {
        User user = user();
        when(userRepository.findActiveByUsername("user01"))
                .thenReturn(Optional.of(user));

        User result = userReader.getActiveByUsername("user01");

        assertThat(result).isSameAs(user);
        verify(userRepository).findActiveByUsername("user01");
    }

    @Test
    @DisplayName("활성 username이 없으면 USER_NOT_FOUND 예외가 발생한다")
    void getActiveByUsername_missingUser_throwsNotFound() {
        when(userRepository.findActiveByUsername("unknown"))
                .thenReturn(Optional.empty());

        BaseException exception = assertThrows(
                BaseException.class,
                () -> userReader.getActiveByUsername("unknown")
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    private void assertDeactivated(User user) {
        BaseException exception = assertThrows(
                BaseException.class,
                () -> userReader.getAccessibleUser(user.getUserId())
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(UserErrorCode.DEACTIVATED_USER);
    }

    private User user() {
        User user = User.create(
                "user01",
                "{bcrypt}encoded-password",
                "U-USER-01",
                UserRole.HUB_ADMIN,
                HUB_ID,
                AffiliationType.HUB
        );
        ReflectionTestUtils.setField(user, "userId", USER_ID);
        return user;
    }
}
