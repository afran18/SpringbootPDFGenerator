package com.ararts.quotation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * JSON body sent by the React app.
 * Only customer + items are mandatory; everything else has a sensible default.
 */
public record QuotationRequest(
        @NotNull(message = "Customer details are required") @Valid CustomerDto customer,
        @NotEmpty(message = "At least one item is required") @Valid List<ItemDto> items,
        @DecimalMin("0") @DecimalMax("100") BigDecimal gstPercent,
        @DecimalMin("0") BigDecimal discount,          // flat amount in rupees
        LocalDate quotationDate,                       // "2026-09-28"
        @Min(1) Integer validForDays,
        @Size(max = 40) String quotationNumber,
        @Size(max = 600) String notes) {
}
