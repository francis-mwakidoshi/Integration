package com.frank.soap;

import java.util.List;

public record FullCountryInfoData(
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
