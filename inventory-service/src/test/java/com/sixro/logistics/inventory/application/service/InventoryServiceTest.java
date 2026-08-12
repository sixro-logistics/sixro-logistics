package com.sixro.logistics.inventory.application.service;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.inventory.application.command.*;
import com.sixro.logistics.inventory.application.common.model.UserRole;
import com.sixro.logistics.inventory.application.result.*;
import com.sixro.logistics.inventory.application.service.event.ProcessedEventService;
import com.sixro.logistics.inventory.application.service.inventory.InventoryCommandService;
import com.sixro.logistics.inventory.application.service.inventory.InventoryQueryService;
import com.sixro.logistics.inventory.domain.entity.inventory.Inventory;
import com.sixro.logistics.inventory.domain.repository.inventory.InventoryRepository;
import com.sixro.logistics.inventory.exception.InventoryErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @InjectMocks
    private InventoryCommandService inventoryService;

    @InjectMocks
    private InventoryQueryService inventoryQueryService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProcessedEventService processedEventService;

    @Test
    @DisplayName("재고 등록 성공")
    void createInventory_success() {

        // given
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Integer stock = 100;

        InventoryCreateServiceCommand createCommand =
                new InventoryCreateServiceCommand(
                        hubId,
                        companyId,
                        productId,
                        stock
                );

        Inventory inventory =
                Inventory.create(
                        hubId,
                        companyId,
                        productId,
                        stock
                );

        ReflectionTestUtils.setField(
                inventory,
                "id",
                UUID.randomUUID()
        );

        given(inventoryRepository.save(any(Inventory.class)))
                .willReturn(inventory);

        // when
        InventoryCreateResult createResult =
                inventoryService.createInventory(createCommand);

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
                Inventory.create(
                        hubId,
                        companyId,
                        productId,
                        100
                );

        ReflectionTestUtils.setField(
                inventory,
                "id",
                inventoryId
        );

        InventoryUpdateCommand command =
                new InventoryUpdateCommand(200);

        given(
                inventoryRepository.findForUpdateByIdAndIsDeletedFalse(
                        inventoryId
                )
        ).willReturn(Optional.of(inventory));

        // when
        InventoryUpdateResult result =
                inventoryService.updateInventory(
                        inventoryId,
                        command
                );

        // then
        assertThat(result).isNotNull();
        assertThat(result.inventoryId()).isEqualTo(inventoryId);
        assertThat(result.stock()).isEqualTo(200);

        verify(inventoryRepository)
                .findForUpdateByIdAndIsDeletedFalse(inventoryId);
    }

    @Test
    @DisplayName("재고 수정 실패 - 재고가 존재하지 않음")
    void updateInventory_fail_inventoryNotFound() {

        // given
        UUID inventoryId = UUID.randomUUID();

        InventoryUpdateCommand command =
                new InventoryUpdateCommand(200);

        given(
                inventoryRepository.findForUpdateByIdAndIsDeletedFalse(
                        inventoryId
                )
        ).willReturn(Optional.empty());

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
                Inventory.create(
                        hubId,
                        companyId,
                        productId,
                        100
                );

        ReflectionTestUtils.setField(
                inventory,
                "id",
                inventoryId
        );

        InventoryStockInCommand command =
                new InventoryStockInCommand(50);

        given(
                inventoryRepository.findForUpdateByIdAndIsDeletedFalse(
                        inventoryId
                )
        ).willReturn(Optional.of(inventory));

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
                .findForUpdateByIdAndIsDeletedFalse(inventoryId);
    }

    @Test
    @DisplayName("재고 입고 실패 - 재고가 존재하지 않음")
    void stockInInventory_fail_inventoryNotFound() {

        // given
        UUID inventoryId = UUID.randomUUID();

        InventoryStockInCommand command =
                new InventoryStockInCommand(50);

        given(
                inventoryRepository.findForUpdateByIdAndIsDeletedFalse(
                        inventoryId
                )
        ).willReturn(Optional.empty());

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

        given(
                inventoryRepository.findByIdAndIsDeletedFalse(
                        inventoryId
                )
        ).willReturn(Optional.of(inventory));

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

        given(
                inventoryRepository.findByIdAndIsDeletedFalse(
                        inventoryId
                )
        ).willReturn(Optional.empty());

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
    @DisplayName("재고 목록 조회 성공")
    void searchInventory_success() {

        // given
        UserRole userRole = UserRole.MASTER_ADMIN;
        UUID affiliationId = UUID.randomUUID();

        InventorySearchCommand command =
                new InventorySearchCommand(
                        null,
                        null,
                        null
                );

        Pageable pageable =
                PageRequest.of(0, 10);

        Inventory inventory1 =
                Inventory.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        100
                );

        Inventory inventory2 =
                Inventory.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        200
                );

        Page<Inventory> inventoryPage =
                new PageImpl<>(
                        List.of(inventory1, inventory2),
                        pageable,
                        2
                );

        given(
                inventoryRepository.findAll(
                        userRole,
                        affiliationId,
                        command,
                        pageable
                )
        ).willReturn(inventoryPage);

        // when
        InventorySearchResult result =
                inventoryQueryService.searchInventory(
                        userRole,
                        affiliationId,
                        command,
                        pageable
                );

        // then
        assertThat(result).isNotNull();
        assertThat(result.page()).isNotNull();
        assertThat(result.page().getContent()).hasSize(2);

        InventorySearchItem firstItem =
                result.page().getContent().get(0);

        assertThat(firstItem.inventoryId())
                .isEqualTo(inventory1.getId());

        assertThat(firstItem.hubId())
                .isEqualTo(inventory1.getHubId());

        assertThat(firstItem.companyId())
                .isEqualTo(inventory1.getCompanyId());

        assertThat(firstItem.productId())
                .isEqualTo(inventory1.getProductId());

        assertThat(firstItem.stock())
                .isEqualTo(inventory1.getStock());

        verify(inventoryRepository)
                .findAll(
                        userRole,
                        affiliationId,
                        command,
                        pageable
                );
    }

    @Test
    @DisplayName("주문 취소 이벤트로 재고 복원 성공 - 이벤트가 처음 처리됨")
    void restoreInventoryByEvent_success() {

        // given
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Inventory inventory =
                Inventory.create(
                        hubId,
                        UUID.randomUUID(),
                        productId,
                        100
                );

        InventoryRestoreByEventCommand command =
                new InventoryRestoreByEventCommand(
                        eventId,
                        orderId,
                        hubId,
                        List.of(
                                new InventoryCommandItem(
                                        productId,
                                        50
                                )
                        )
                );

        given(processedEventService.isProcessed(eventId))
                .willReturn(false);

        given(
                inventoryRepository
                        .findAllForUpdateByHubIdAndProductIdInAndIsDeletedFalse(
                                hubId,
                                List.of(productId)
                        )
        ).willReturn(List.of(inventory));

        // when
        inventoryService.restoreInventoryByEvent(command);

        // then
        assertThat(inventory.getStock())
                .isEqualTo(150);

        verify(processedEventService)
                .isProcessed(eventId);

        verify(processedEventService)
                .save(eventId);

        verify(inventoryRepository)
                .findAllForUpdateByHubIdAndProductIdInAndIsDeletedFalse(
                        hubId,
                        List.of(productId)
                );
    }

    @Test
    @DisplayName("주문 취소 이벤트 중복 처리 - 이미 처리된 이벤트")
    void restoreInventoryByEvent_duplicateEvent() {

        // given
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Inventory inventory =
                Inventory.create(
                        hubId,
                        UUID.randomUUID(),
                        productId,
                        100
                );

        InventoryRestoreByEventCommand command =
                new InventoryRestoreByEventCommand(
                        eventId,
                        orderId,
                        hubId,
                        List.of(
                                new InventoryCommandItem(
                                        productId,
                                        50
                                )
                        )
                );

        given(processedEventService.isProcessed(eventId))
                .willReturn(true);

        // when
        inventoryService.restoreInventoryByEvent(command);

        // then
        assertThat(inventory.getStock())
                .isEqualTo(100);

        verify(processedEventService)
                .isProcessed(eventId);

        verify(processedEventService, org.mockito.Mockito.never())
                .save(any(UUID.class));

        verify(
                inventoryRepository,
                org.mockito.Mockito.never()
        ).findAllForUpdateByHubIdAndProductIdInAndIsDeletedFalse(
                any(UUID.class),
                anyList()
        );
    }

}