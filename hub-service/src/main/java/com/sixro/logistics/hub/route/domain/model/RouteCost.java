package com.sixro.logistics.hub.route.domain.model;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.route.domain.exception.HubRouteErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteCost {

    @Column(name = "base_cost", nullable = false)
    private int baseCost;

    @Column(name = "toll_fee", nullable = false)
    private int tollFee;

    public RouteCost(int baseCost, int tollFee) {
        validate(baseCost, tollFee);
        this.baseCost = baseCost;
        this.tollFee = tollFee;
    }

    private void validate(int baseCost, int tollFee) {
        if (baseCost < 0 || tollFee < 0) {
            throw new BaseException(HubRouteErrorCode.INVALID_COST);
        }
    }
}