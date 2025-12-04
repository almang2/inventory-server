package com.almang.inventory.wholesale.dto.response;

import java.time.LocalDate;

public record ConfirmWholesaleResponse(
        Long wholesaleId,
        boolean confirmed,
        LocalDate releaseDate
) {}

