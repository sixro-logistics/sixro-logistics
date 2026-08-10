package com.sixro.logistics.order.application.port;

import com.sixro.logistics.order.application.model.CompanyInfo;
import java.util.UUID;

public interface CompanyQueryPort {

    CompanyInfo getCompany(UUID receiverCompanyId);

}
