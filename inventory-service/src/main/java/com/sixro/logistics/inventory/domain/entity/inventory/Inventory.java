package com.sixro.logistics.inventory.domain.entity.inventory;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.common.persistence.entity.BaseEntity;
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
        schema = "inventory_schema"
        */
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inventory_id", updatable = false)
    private UUID id;

    @Column(name = "hub_id", nullable = false, updatable = false)
    private UUID hubId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false)
    private Integer stock;

    private Inventory(UUID hubId, UUID companyId, UUID productId, Integer stock){
        this.hubId = hubId;
        this.companyId = companyId;
        this.productId = productId;
        this.stock = stock;
    }

    public static Inventory create(UUID hubId, UUID companyId, UUID productId, Integer stock){
        return new Inventory(hubId, companyId, productId, stock);
    }

    public void updateStock(Integer stock){
        if(stock == null || stock < 0){
            throw new BaseException(InventoryErrorCode.INVALID_STOCK);
        }
        this.stock = stock;
    }

    public void addStock(Integer quantity) {
        if(quantity == null || quantity <= 0){
            throw new BaseException(InventoryErrorCode.INVALID_STOCK);
        }
        this.stock += quantity;
    }

    public void deductStock(Integer quantity){
        if(quantity == null || quantity < 1){
            throw new BaseException(InventoryErrorCode.INVALID_QUANTITY);
        }

        if(stock < quantity){
            throw new BaseException(InventoryErrorCode.OUT_OF_STOCK);
        }
        stock -= quantity;
    }

    public void restoreStock(Integer quantity) {
        if(quantity == null || quantity < 1){
            throw new BaseException(InventoryErrorCode.INVALID_QUANTITY);
        }
        stock += quantity;
    }

}