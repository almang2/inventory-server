package com.almang.inventory.vendor.dto.request;

import com.almang.inventory.vendor.domain.VendorChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateVendorRequest(
        @NotBlank String name,
        @NotNull VendorChannel channel,
        @NotBlank String contactPoint,
        String note
) {}
