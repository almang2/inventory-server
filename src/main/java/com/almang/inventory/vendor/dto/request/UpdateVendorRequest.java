package com.almang.inventory.vendor.dto.request;

import com.almang.inventory.vendor.domain.VendorChannel;
import jakarta.validation.constraints.NotNull;

public record UpdateVendorRequest(
        @NotNull Long vendorId,
        String name,
        VendorChannel channel,
        String contactPoint,
        String note,
        Boolean activated
) {}
