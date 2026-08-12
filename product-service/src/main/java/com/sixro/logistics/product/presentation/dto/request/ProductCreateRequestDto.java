package com.sixro.logistics.product.presentation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ProductCreateRequestDto {
        @NotNull(message = "소속 업체 ID는 필수입니다.")
        private UUID companyId;

        @NotNull(message = "소속 허브 ID는 필수입니다.")
        private UUID hubId;

        @NotBlank(message = "상품명은 필수 입력 항목입니다.")
        @Size(max = 100, message = "상품명은 100자 이하로 입력해주세요.")
        private String productName;

        @Size(max = 500, message = "상품 설명은 500자 이하로 입력해주세요.")
        private String description;

        @NotNull(message = "상품 가격은 필수 입력 항목입니다.")
        @DecimalMin(value = "0.0", inclusive = true, message = "가격은 0원 이상이어야 합니다.")
        private BigDecimal price;
}
