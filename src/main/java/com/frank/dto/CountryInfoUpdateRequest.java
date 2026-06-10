package com.frank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CountryInfoUpdateRequest(
        @NotBlank(message = "Country name is required")
        @Size(min = 2, max = 100)
        String name,
        String capitalCity,
        String phoneCode,
        String continentCode,
        String currencyCode,
        String countryFlagUrl,
        List<@NotBlank String> languages
) {
}
