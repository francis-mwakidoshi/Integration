package com.frank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CountryNameRequest(
        @NotBlank(message = "Country name is required")
        @Size(min = 2, max = 100, message = "Country name must be between 2 and 100 characters")
        String name
) {
}
