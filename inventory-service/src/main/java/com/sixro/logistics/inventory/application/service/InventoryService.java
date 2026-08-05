package com.sixro.logistics.inventory.application.service;

import com.sixro.logistics.inventory.domain.entity.Inventory;
import com.sixro.logistics.inventory.domain.repository.InventoryRepository;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryCreateRequestDto;
import com.sixro.logistics.inventory.presentation.dto.response.InventoryCreateResponseDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public InventoryCreateResponseDto createInventory(InventoryCreateRequestDto requestDto) {

        Inventory inventory = Inventory.create(
                requestDto.hubId(),
                requestDto.productId(),
                requestDto.stock()
        );

        Inventory createdInventory = inventoryRepository.save(inventory);
        return InventoryCreateResponseDto.from(createdInventory);
    }

}
