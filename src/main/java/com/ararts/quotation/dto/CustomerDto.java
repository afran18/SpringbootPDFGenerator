package com.ararts.quotation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerDto(
        @NotBlank(message = "Customer name is required")
        @Size(max = 120) String name,
        @Size(max = 30) String phone,
        @Email(message = "Customer email is invalid") String email,
        @Size(max = 300) String address,
        @Size(max = 20) String gstin) {
}
