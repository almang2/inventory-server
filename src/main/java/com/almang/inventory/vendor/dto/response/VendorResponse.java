package com.almang.inventory.vendor.dto.response;

import com.almang.inventory.vendor.domain.Vendor;
import com.almang.inventory.vendor.domain.VendorChannel;

public record VendorResponse(
        Long vendorId,
        String name,
        VendorChannel channel,
        String phoneNumber,
        String email,
        String webPage,
        String orderMethod,
        String note,
        boolean activated,
        Long storeId
) {
    public static VendorResponse from(Vendor vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getName(),
                vendor.getChannel(),
                vendor.getPhoneNumber(),
                vendor.getEmail(),
                vendor.getWebPage(),
                vendor.getOrderMethod(),
                vendor.getNote(),
                vendor.isActivated(),
                vendor.getStore().getId()
        );
    }
}
