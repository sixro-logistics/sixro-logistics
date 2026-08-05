package com.sixro.logistics.inventory.application.facade;

import com.sixro.logistics.inventory.application.service.InventoryService;
import com.sixro.logistics.inventory.infrastructure.client.hub.HubClient;
import com.sixro.logistics.inventory.infrastructure.client.hub.HubClientResponse;
import com.sixro.logistics.inventory.infrastructure.client.product.ProductClient;
import com.sixro.logistics.inventory.infrastructure.client.product.ProductClientResponse;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryCreateRequestDto;
import com.sixro.logistics.inventory.presentation.dto.response.InventoryCreateResponseDto;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryFacade {

    private final InventoryService inventoryService;
    private final HubClient hubClient;
    private final ProductClient productClient;

    @Transactional
    public InventoryCreateResponseDto createInventory(@Valid InventoryCreateRequestDto requestDto) {
        /*
        HubClientResponse hub = hubClient.getHub(requestDto.hubId());
        ProductClientResponse product = productClient.getProduct(requestDto.productId());
        validateAuthority(hub, product);
        */
        return inventoryService.createInventory(requestDto);
    }

    private void validateAuthority(HubClientResponse hub, ProductClientResponse product) {
    }
}
