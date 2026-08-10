package com.sixro.logistics.order.infrastructure.client.inventory;

import com.sixro.logistics.common.core.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", path = "/api/v1")
public interface InventoryClient {

    // TO DO: 응답 시 CommonResponse에 감싸서 받기
    // 하나의 상품이 여러 허브에서 관리될 수 있어서 허브 id와 상품 id 목록을 함께 전달
    @PostMapping("/inventories/check")
    InventoryClientResponse getInventories(@RequestBody InventoryCheckRequest checkRequest);

}
