package com.sixro.logistics.order.infrastructure.client.inventory;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;


@FeignClient(name = "inventory-service", path = "/api/v1")
public interface InventoryClient {

    // 하나의 상품이 여러 허브에서 관리될 수 있으니, 허브, 상품 id 리스트를 같이 넘겨야 함
    @PostMapping("/inventories/check")
    InventoryClientResponse getInventories(@RequestBody InventoryCheckRequest checkRequest);

}
