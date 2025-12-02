package com.almang.inventory.vendor.dto.request;

import com.almang.inventory.vendor.domain.VendorChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVendorRequest(
        @NotBlank @Size(max = 30) String name,
        @NotNull VendorChannel channel,
        @Size(max = 30) String phoneNumber,
        @Size(max = 30) String email,
        String webPage,
        String orderMethod,
        String note
) {}
