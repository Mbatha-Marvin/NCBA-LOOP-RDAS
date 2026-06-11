package com.ncba.loop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryResponse {
    private String isoCode;
    private String name;
    private String capitalCity;
    private String phoneCode;
    private String continentCode;
    private String continentName;
    private String currencyIsoCode;
    private String currencyName;
    private String countryFlag;
    private List<LanguageDto> languages;
}
