package com.sixro.logistics.delivery.infrastructure.client.company;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.delivery.application.model.CompanyHubInfo;
import com.sixro.logistics.delivery.application.port.CompanyQueryPort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompanyClientAdapter implements CompanyQueryPort {

    private final CompanyClient companyClient;

    @Override
    public Optional<CompanyHubInfo> findHubInfo(UUID companyId) {
        try {
            CommonResponse<CompanyClientResponse> response = companyClient.getHubInfo(companyId);

            if (response == null || !response.success() || response.data() == null) {
                throw new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR, new IllegalStateException("company-service가 유효하지 않은 응답을 반환했습니다."));
            }

            CompanyClientResponse data = response.data();

            return Optional.of(new CompanyHubInfo(data.destCompanyId(), data.destHubId()));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }
}
