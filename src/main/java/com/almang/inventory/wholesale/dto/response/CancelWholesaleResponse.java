package com.almang.inventory.wholesale.dto.response;

public record CancelWholesaleResponse(
        Long wholesaleId,
        boolean canceled
) {}

