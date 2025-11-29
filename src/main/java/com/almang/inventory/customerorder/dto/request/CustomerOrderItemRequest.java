package com.almang.inventory.customerorder.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerOrderItemRequest {

    @NotBlank
    @JsonProperty("product_code")
    private String productCode;

    @NotBlank
    @JsonProperty("product_name")
    private String productName;

    @NotNull
    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("option_value")
    private String optionValue;

    @JsonProperty("variant_code")
    private String variantCode;

    @JsonProperty("item_code")
    private String itemCode;
}
