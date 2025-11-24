package com.almang.inventory.receipt.dto.request;

import com.almang.inventory.receipt.domain.ReceiptStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record UpdateReceiptRequest(
        @NotNull Long orderId,
        Integer totalBoxCount,
        BigDecimal totalWeightG,
        ReceiptStatus status,
        Boolean activated,
        @Valid List<UpdateReceiptItemRequest> orderItems
) {}
