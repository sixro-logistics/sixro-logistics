package com.sixro.logistics.inventory.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.inventory.application.command.*;
import com.sixro.logistics.inventory.application.result.InventoryCreateResult;
import com.sixro.logistics.inventory.application.result.InventoryDeleteResult;
import com.sixro.logistics.inventory.application.result.InventoryStockInCompanyResult;
import com.sixro.logistics.inventory.application.result.InventoryUpdateResult;
import com.sixro.logistics.inventory.application.service.inventory.InventoryCommandService;
import com.sixro.logistics.inventory.application.service.outbox.OutboxService;
import com.sixro.logistics.inventory.domain.entity.inventory.Inventory;
import com.sixro.logistics.inventory.domain.event.InventoryDeductedEvent;
import com.sixro.logistics.inventory.domain.event.InventoryDeductionFailedEvent;
import com.sixro.logistics.inventory.domain.repository.inventory.InventoryRepository;
import com.sixro.logistics.inventory.exception.InventoryErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @InjectMocks
    private InventoryCommandService inventoryService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OutboxService outboxService;

    @Test
    @DisplayName("재고 등록 성공")
    void createInventory_success(){

        // given
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Integer stock = 100;

        InventoryCreateServiceCommand createCommand
                = new InventoryCreateServiceCommand(hubId, companyId, productId, stock);

        Inventory inventory
                = Inventory.create(hubId, companyId, productId, stock);

        ReflectionTestUtils.setField(
                inventory,
                "id",
                UUID.randomUUID()
        );

        given(inventoryRepository.save(any(Inventory.class)))
                .willReturn(inventory);

        // when
        InventoryCreateResult createResult
                = inventoryService.createInventory(createCommand);

        // then
        assertThat(createResult).isNotNull();
        assertThat(createResult.inventoryId()).isNotNull();
        assertThat(createResult.hubId()).isEqualTo(createCommand.hubId());
        assertThat(createResult.companyId()).isEqualTo(createCommand.companyId());
        assertThat(createResult.productId()).isEqualTo(createCommand.productId());
        assertThat(createResult.stock()).isEqualTo(createCommand.stock());

        ArgumentCaptor<Inventory> captor =
                ArgumentCaptor.forClass(Inventory.class);

        verify(inventoryRepository).save(captor.capture());

        Inventory saved = captor.getValue();

        assertThat(saved.getHubId()).isEqualTo(hubId);
        assertThat(saved.getCompanyId()).isEqualTo(companyId);
        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(saved.getStock()).isEqualTo(stock);
    }

    @Test
    @DisplayName("재고 수정 성공")
    void updateInventory_success() {

        // given
        UUID inventoryId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Inventory inventory =
                Inventory.create(hubId, companyId, productId, 100);

        ReflectionTestUtils.setField(
                inventory,
                "id",
                inventoryId
        );

        InventoryUpdateCommand command =
                new InventoryUpdateCommand(200);

        given(inventoryRepository.findByIdAndIsDeletedFalse(inventoryId))
                .willReturn(Optional.of(inventory));

        // when
        InventoryUpdateResult result =
                inventoryService.updateInventory(inventoryId, command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.inventoryId()).isEqualTo(inventoryId);
        assertThat(result.stock()).isEqualTo(200);

        verify(inventoryRepository)
                .findByIdAndIsDeletedFalse(inventoryId);
    }

    @Test
    @DisplayName("재고 수정 실패 - 재고가 존재하지 않음")
    void updateInventory_fail_inventoryNotFound() {

        // given
        UUID inventoryId = UUID.randomUUID();

        InventoryUpdateCommand command =
                new InventoryUpdateCommand(200);

        given(inventoryRepository.findByIdAndIsDeletedFalse(inventoryId))
                .willReturn(Optional.empty());

        // when
        BaseException exception = catchThrowableOfType(
                () -> inventoryService.updateInventory(
                        inventoryId,
                        command
                ),
                BaseException.class
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(InventoryErrorCode.INVENTORY_NOT_FOUND);
    }

    @Test
    @DisplayName("재고 입고 성공")
    void stockInInventory_success() {

        // given
        UUID inventoryId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Inventory inventory =
                Inventory.create(hubId, companyId, productId, 100);

        ReflectionTestUtils.setField(
                inventory,
                "id",
                inventoryId
        );

        InventoryStockInCommand command =
                new InventoryStockInCommand(50);

        given(inventoryRepository.findByIdAndIsDeletedFalse(inventoryId))
                .willReturn(Optional.of(inventory));

        // when
        InventoryStockInCompanyResult result =
                inventoryService.stockInInventory(
                        inventoryId,
                        command
                );

        // then
        assertThat(result).isNotNull();
        assertThat(result.inventoryId()).isEqualTo(inventoryId);
        assertThat(result.stock()).isEqualTo(150);

        verify(inventoryRepository)
                .findByIdAndIsDeletedFalse(inventoryId);
    }

    @Test
    @DisplayName("재고 입고 실패 - 재고가 존재하지 않음")
    void stockInInventory_fail_inventoryNotFound() {

        // given
        UUID inventoryId = UUID.randomUUID();

        InventoryStockInCommand command =
                new InventoryStockInCommand(50);

        given(inventoryRepository.findByIdAndIsDeletedFalse(inventoryId))
                .willReturn(Optional.empty());

        // when
        BaseException exception = catchThrowableOfType(
                () -> inventoryService.stockInInventory(
                        inventoryId,
                        command
                ),
                BaseException.class
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(InventoryErrorCode.INVENTORY_NOT_FOUND);
    }

    @Test
    @DisplayName("재고 삭제 성공")
    void deleteInventory_success() {

        // given
        UUID inventoryId = UUID.randomUUID();

        Inventory inventory =
                Inventory.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        100
                );

        ReflectionTestUtils.setField(
                inventory,
                "id",
                inventoryId
        );

        given(inventoryRepository.findByIdAndIsDeletedFalse(inventoryId))
                .willReturn(Optional.of(inventory));

        // when
        InventoryDeleteResult result =
                inventoryService.deleteInventory(inventoryId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.inventoryId())
                .isEqualTo(inventoryId);

        verify(inventoryRepository)
                .findByIdAndIsDeletedFalse(inventoryId);
    }

    @Test
    @DisplayName("재고 삭제 실패 - 재고가 존재하지 않음")
    void deleteInventory_fail_inventoryNotFound() {

        // given
        UUID inventoryId = UUID.randomUUID();

        given(inventoryRepository.findByIdAndIsDeletedFalse(inventoryId))
                .willReturn(Optional.empty());

        // when
        BaseException exception = catchThrowableOfType(
                () -> inventoryService.deleteInventory(inventoryId),
                BaseException.class
        );

        // then
        assertThat(exception.getErrorCode())
                .isEqualTo(InventoryErrorCode.INVENTORY_NOT_FOUND);
    }

    @Test
    @DisplayName("재고 차감 성공 - Outbox 이벤트 생성")
    void deductInventory_success() {

        // given
        UUID orderId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InventoryDeductItem item =
                new InventoryDeductItem(
                        productId,
                        50
                );

        InventoryDeductCommand command =
                new InventoryDeductCommand(
                        orderId,
                        hubId,
                        List.of(item)
                );

        Inventory inventory =
                Inventory.create(
                        hubId,
                        UUID.randomUUID(),
                        productId,
                        100
                );

        given(
                inventoryRepository
                        .findAllByHubIdAndProductIdInAndIsDeletedFalse(
                                eq(hubId),
                                anyList()
                        )
        ).willReturn(List.of(inventory));

        // when
        inventoryService.deductInventory(command);

        // then
        assertThat(inventory.getStock())
                .isEqualTo(50);

        ArgumentCaptor<InventoryDeductedEvent> captor =
                ArgumentCaptor.forClass(InventoryDeductedEvent.class);

        verify(outboxService)
                .save(captor.capture());

        InventoryDeductedEvent event =
                captor.getValue();

        assertThat(event.orderId())
                .isEqualTo(orderId);
    }

    @Test
    @DisplayName("재고 차감 실패 - 재고 부족")
    void deductInventory_fail_outOfStock() {

        // given
        UUID orderId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InventoryDeductItem item =
                new InventoryDeductItem(
                        productId,
                        200
                );

        InventoryDeductCommand command =
                new InventoryDeductCommand(
                        orderId,
                        hubId,
                        List.of(item)
                );

        Inventory inventory =
                Inventory.create(
                        hubId,
                        UUID.randomUUID(),
                        productId,
                        180
                );

        given(
                inventoryRepository
                        .findAllByHubIdAndProductIdInAndIsDeletedFalse(
                                eq(hubId),
                                anyList()
                        )
        ).willReturn(List.of(inventory));

        // when
        inventoryService.deductInventory(command);

        // then
        assertThat(inventory.getStock())
                .isEqualTo(180);

        ArgumentCaptor<InventoryDeductionFailedEvent> captor =
                ArgumentCaptor.forClass(
                        InventoryDeductionFailedEvent.class
                );

        verify(outboxService)
                .save(captor.capture());

        InventoryDeductionFailedEvent event =
                captor.getValue();

        assertThat(event.orderId())
                .isEqualTo(orderId);

        assertThat(event.errorCode())
                .isEqualTo(
                        InventoryErrorCode.OUT_OF_STOCK.getCode()
                );

        assertThat(event.failureReason())
                .isEqualTo(
                        InventoryErrorCode.OUT_OF_STOCK.getMessage()
                );
    }

}