package com.sixro.logistics.inventory.domain.entity;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.inventory.exception.InventoryErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "p_inventory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_hub_product",
                        columnNames = {"hub_id", "product_id"}
                )
        }/*,
        schema = ""
        */
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Inventory /* extends BaseEntity */{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inventory_id", updatable = false)
    private UUID id;

    @Column(name = "hub_id", nullable = false, updatable = false)
    private UUID hubId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false)
    private Integer stock;

    @Version
    private Long version;

    private Inventory(UUID hubId, UUID productId, Integer stock){
        this.hubId = hubId;
        this.productId = productId;
        this.stock = stock;
    }

    public static Inventory create(UUID hubId, UUID productId, Integer stock){
        return new Inventory(hubId, productId, stock);
    }

    public void decreaseHubStock(Integer quantity){
        if(stock < quantity){
            throw new BaseException(InventoryErrorCode.OUT_OF_STOCK);
        }
        stock -= quantity;
    }

}