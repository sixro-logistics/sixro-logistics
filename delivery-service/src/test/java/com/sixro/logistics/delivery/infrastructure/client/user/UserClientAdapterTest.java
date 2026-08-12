package com.sixro.logistics.delivery.infrastructure.client.user;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.delivery.application.model.UserInfo;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserClientAdapterTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID AFFILIATION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private UserClient userClient;

    @InjectMocks
    private UserClientAdapter adapter;

    @Test
    @DisplayName("User Service의 정상 응답을 Application 사용자 정보로 변환한다")
    void findUser_success_mapsUserInfo() {
        // given
        UserClientResponse responseData = new UserClientResponse(
                USER_ID, "홍길동", "DELIVERY_MANAGER", "APPROVED",
                "U-DELIVERY", AFFILIATION_ID, "HUB"
        );
        when(userClient.getDeliveryInfo(USER_ID))
                .thenReturn(CommonResponse.success("사용자 조회 성공", responseData));

        // when
        Optional<UserInfo> result = adapter.findUser(USER_ID);

        // then
        assertThat(result).contains(new UserInfo(
                USER_ID, "홍길동", "DELIVERY_MANAGER", "APPROVED",
                "U-DELIVERY", AFFILIATION_ID, "HUB"
        ));
        verify(userClient).getDeliveryInfo(USER_ID);
    }

    @Test
    @DisplayName("User Service가 404를 반환하면 빈 Optional을 반환한다")
    void findUser_notFound_returnsEmpty() {
        // given
        when(userClient.getDeliveryInfo(USER_ID))
                .thenThrow(mock(FeignException.NotFound.class));

        // when
        Optional<UserInfo> result = adapter.findUser(USER_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("User Service 응답 데이터가 없으면 C999 예외가 발생한다")
    void findUser_missingData_throwsInternalServerError() {
        // given
        when(userClient.getDeliveryInfo(USER_ID))
                .thenReturn(CommonResponse.success("사용자 조회 성공", null));

        // when
        BaseException exception = catchThrowableOfType(
                BaseException.class,
                () -> adapter.findUser(USER_ID)
        );

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
    }
}
