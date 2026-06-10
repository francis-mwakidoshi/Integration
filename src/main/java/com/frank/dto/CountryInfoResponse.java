package com.frank.dto;

import java.util.List;

public record CountryInfoResponse(
        Long id,
        String isoCode,
        String name,
        String capitalCity,
        String phoneCode,
        String continentCode,
        String currencyCode,
        String countryFlagUrl,
        List<String> languages
) {
}
