package com.almang.inventory.wholesale.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreatePendingWholesaleRequest(
        String orderReference,
        @NotEmpty(message = "출고 항목이 비어있습니다.")
        @Valid
        List<CreateWholesaleItemRequest> items
) {}

