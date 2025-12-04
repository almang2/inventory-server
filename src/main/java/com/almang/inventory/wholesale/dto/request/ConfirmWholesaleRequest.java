package com.almang.inventory.wholesale.dto.request;

import java.time.LocalDate;

public record ConfirmWholesaleRequest(
        LocalDate releaseDate
) {}

