package com.almang.inventory.wholesale.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateWholesaleItemRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,
        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 0보다 커야 합니다.")
        BigDecimal quantity,
        @Positive(message = "단가는 0보다 커야 합니다.")
        Integer unitPrice,
        String note
) {}

