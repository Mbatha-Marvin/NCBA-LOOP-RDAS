package com.ncba.loop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryDetailResponse {
    private String isoCode;
    private String name;
    private String capitalCity;
    private String phoneCode;
    private String continentCode;
    private String continentName;
    private String currencyIsoCode;
    private String currencyName;
    private String countryFlag;
    private java.util.List<LanguageDto> languages;
    private java.util.List<CountrySummaryDto> countriesSharingCurrency;
}
