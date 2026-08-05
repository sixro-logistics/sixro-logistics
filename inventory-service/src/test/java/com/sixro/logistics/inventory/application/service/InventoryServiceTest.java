package com.sixro.logistics.inventory.application.service;

import com.sixro.logistics.inventory.domain.entity.Inventory;
import com.sixro.logistics.inventory.domain.repository.InventoryRepository;
import com.sixro.logistics.inventory.presentation.dto.request.InventoryCreateRequestDto;
import com.sixro.logistics.inventory.presentation.dto.response.InventoryCreateResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @InjectMocks
    private InventoryService inventoryService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("재고 등록 성공")
    void createInventory_success(){

        // given
        UUID hubId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Integer stock = 100;

        InventoryCreateRequestDto requestDto
                = new InventoryCreateRequestDto(hubId, productId, stock);

        Inventory inventory
                = Inventory.create(hubId, productId, stock);

        ReflectionTestUtils.setField(
                inventory,
                "id",
                UUID.randomUUID()
        );

        given(inventoryRepository.save(any(Inventory.class)))
                .willReturn(inventory);

        // when
        InventoryCreateResponseDto responseDto
                = inventoryService.createInventory(requestDto);

        // then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.inventoryId()).isNotNull();
        assertThat(responseDto.hubId()).isEqualTo(requestDto.hubId());
        assertThat(responseDto.productId()).isEqualTo(requestDto.productId());
        assertThat(responseDto.stock()).isEqualTo(requestDto.stock());

        ArgumentCaptor<Inventory> captor =
                ArgumentCaptor.forClass(Inventory.class);

        verify(inventoryRepository).save(captor.capture());

        Inventory saved = captor.getValue();

        assertThat(saved.getHubId()).isEqualTo(hubId);
        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(saved.getStock()).isEqualTo(stock);
    }

}