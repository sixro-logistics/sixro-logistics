package com.sixro.logistics.order.application.facade.order;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.core.exception.CommonErrorCode;
import com.sixro.logistics.order.application.command.OrderSearchCommand;
import com.sixro.logistics.order.application.result.OrderGetOneResult;
import com.sixro.logistics.order.application.result.OrderSearchResult;
import com.sixro.logistics.order.application.service.order.OrderQueryService;
import com.sixro.logistics.order.common.model.UserRole;
import com.sixro.logistics.order.exception.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderQueryFacade {

    private final OrderQueryService orderQueryService;

    public OrderGetOneResult getOneOrder(
            UserRole userRole, UUID affiliationId, UUID orderId
    ) {

        OrderGetOneResult result = orderQueryService.getOneOrder(orderId);

        if(userRole == UserRole.HUB_ADMIN){
            if(!affiliationId.equals(result.hubId())){
                throw new BaseException(CommonErrorCode.FORBIDDEN);
            }
        }

        boolean isReceiverCompany = affiliationId.equals(result.receiverCompanyId());

        boolean isSupplierCompany =
                result.orderItems().stream()
                        .anyMatch(item ->
                                affiliationId.equals(item.companyId()));

        if(!isReceiverCompany || !isSupplierCompany){
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }

        // TO DO : 배송 담당자 API 호출 및 권한 검증

        return result;
    }

    public OrderSearchResult searchOrder(
            UUID userId, UserRole userRole, UUID affiliationId,
            OrderSearchCommand command, Pageable pageable) {

        Pageable validatedPageable = pageValidate(pageable);

        return orderQueryService.search(userId, userRole, affiliationId, command, validatedPageable);
    }

    private Pageable pageValidate(Pageable pageable){
        Set<String> sortList = Set.of("createdAt", "updatedAt");

        for(Sort.Order order : pageable.getSort()){
            if(!sortList.contains(order.getProperty())){
                throw new BaseException(OrderErrorCode.INVALID_SORT_FIELD);
            }
        }

        int size = pageable.getPageSize();
        if(size != 10 && size != 30 && size != 50){
            return PageRequest.of(
                    pageable.getPageNumber(),
                    10,
                    pageable.getSort()
            );
        }

        return pageable;
    }

}
