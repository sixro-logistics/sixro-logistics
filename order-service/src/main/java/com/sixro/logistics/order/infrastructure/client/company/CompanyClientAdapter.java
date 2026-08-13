package com.sixro.logistics.order.infrastructure.client.company;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.response.CommonResponse;
import com.sixro.logistics.order.application.model.CompanyInfo;
import com.sixro.logistics.order.application.model.HubInfo;
import com.sixro.logistics.order.application.port.CompanyQueryPort;
import com.sixro.logistics.order.exception.OrderErrorCode;
import com.sixro.logistics.order.infrastructure.client.hub.HubClientResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompanyClientAdapter implements CompanyQueryPort {

    private final CompanyClient companyClient;

    @Override
    public CompanyInfo getCompany(UUID receiverCompanyId) {

        try {
            CommonResponse<CompanyClientResponse> response
                    = companyClient.getCompany(receiverCompanyId);

            return new CompanyInfo(
                    response.data().companyId(),
                    response.data().address(),
                    response.data().hubId()
            );

        } catch (FeignException.NotFound e) {
            throw new BaseException(OrderErrorCode.COMPANY_NOT_FOUND);
        }

    }
}
