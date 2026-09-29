package com.ararts.quotation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * One row of the quotation. amount = quantity x rate is calculated by the
 * backend, so the frontend cannot send a wrong line total.
 */
public record ItemDto(
        @NotBlank(message = "Item name is required")
        @Size(max = 150) String name,
        @Size(max = 400) String description,
        @Size(max = 40) String size,          // e.g. "10 x 4 ft"
        @Size(max = 20) String unit,          // e.g. "sq.ft", "nos"
        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0", inclusive = false, message = "Quantity must be greater than 0")
        BigDecimal quantity,
        @NotNull(message = "Rate is required")
        @DecimalMin(value = "0", message = "Rate cannot be negative")
        BigDecimal rate) {
}
