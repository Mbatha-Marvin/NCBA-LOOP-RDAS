package com.ncba.loop.service;

import com.ncba.loop.client.CountryInfoSoapClient;
import com.ncba.loop.client.CountryInfoSoapClient.*;
import com.ncba.loop.config.CacheConfig;
import com.ncba.loop.dto.*;
import com.ncba.loop.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CountryService {

    private static final Logger log = LoggerFactory.getLogger(CountryService.class);

    private final CountryInfoSoapClient soapClient;
    private final ConcurrentHashMap<String, String> continentCodeToName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> currencyCodeToName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> languageCodeToName = new ConcurrentHashMap<>();

    public CountryService(CountryInfoSoapClient soapClient) {
        this.soapClient = soapClient;
    }

    @Cacheable(value = CacheConfig.CACHE_CONTINENTS)
    public List<ContinentDto> getContinents() {
        List<ContinentResult> results = soapClient.listContinentsByName();
        return results.stream()
                .map(r -> {
                    continentCodeToName.put(r.code(), r.name());
                    return ContinentDto.builder().code(r.code()).name(r.name()).build();
                })
                .collect(Collectors.toList());
    }

    @Cacheable(value = CacheConfig.CACHE_CURRENCIES)
    public List<CurrencyDto> getCurrencies() {
        List<CurrencyResult> results = soapClient.listCurrenciesByName();
        return results.stream()
                .map(r -> {
                    currencyCodeToName.put(r.isoCode(), r.name());
                    return CurrencyDto.builder().isoCode(r.isoCode()).name(r.name()).build();
                })
                .collect(Collectors.toList());
    }

    @Cacheable(value = CacheConfig.CACHE_LANGUAGES)
    public List<LanguageDto> getLanguages() {
        List<LanguageResult> results = soapClient.listLanguagesByName();
        return results.stream()
                .map(r -> {
                    languageCodeToName.put(r.isoCode(), r.name());
                    return LanguageDto.builder().isoCode(r.isoCode()).name(r.name()).build();
                })
                .collect(Collectors.toList());
    }

    public PagedResponse<CountryResponse> searchCountries(String name, String continent, String currency,
                                                           String language, int page, int size, String sort) {
        List<FullCountryResult> allCountries = getAllCountries();
        ensureLookupMapsPopulated();

        List<CountryResponse> filtered = allCountries.stream()
                .filter(c -> name == null || name.isBlank() || c.name().toLowerCase().contains(name.toLowerCase().trim()))
                .filter(c -> continent == null || continent.isBlank() || c.continentCode().equalsIgnoreCase(continent.trim()))
                .filter(c -> currency == null || currency.isBlank() || c.currencyIsoCode().equalsIgnoreCase(currency.trim()))
                .filter(c -> language == null || language.isBlank() || hasLanguage(c, language.trim()))
                .map(this::toCountryResponse)
                .collect(Collectors.toList());

        filtered = applySort(filtered, sort);

        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<CountryResponse> pageContent = fromIndex < totalElements
                ? filtered.subList(fromIndex, toIndex)
                : Collections.emptyList();

        return PagedResponse.<CountryResponse>builder()
                .content(pageContent)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }

    public CountryDetailResponse getCountryDetail(String isoCode) {
        List<FullCountryResult> allCountries = getAllCountries();
        ensureLookupMapsPopulated();

        FullCountryResult country = allCountries.stream()
                .filter(c -> c.isoCode().equalsIgnoreCase(isoCode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Country not found: " + isoCode));

        List<CountryNameResult> sharingCurrency = soapClient.countriesUsingCurrency(country.currencyIsoCode());
        List<CountrySummaryDto> sharing = sharingCurrency.stream()
                .filter(c -> !c.isoCode().equalsIgnoreCase(isoCode))
                .map(c -> CountrySummaryDto.builder().isoCode(c.isoCode()).name(c.name()).build())
                .collect(Collectors.toList());

        return CountryDetailResponse.builder()
                .isoCode(country.isoCode())
                .name(country.name())
                .capitalCity(country.capitalCity())
                .phoneCode(country.phoneCode())
                .continentCode(country.continentCode())
                .continentName(continentCodeToName.getOrDefault(country.continentCode(), country.continentCode()))
                .currencyIsoCode(country.currencyIsoCode())
                .currencyName(currencyCodeToName.getOrDefault(country.currencyIsoCode(), country.currencyIsoCode()))
                .countryFlag(country.countryFlag())
                .languages(country.languages().stream()
                        .map(l -> LanguageDto.builder()
                                .isoCode(l.isoCode())
                                .name(languageCodeToName.getOrDefault(l.isoCode(), l.name()))
                                .build())
                        .collect(Collectors.toList()))
                .countriesSharingCurrency(sharing)
                .build();
    }

    public List<CountrySummaryDto> getCountriesSharingCurrency(String isoCode) {
        List<FullCountryResult> allCountries = getAllCountries();

        FullCountryResult country = allCountries.stream()
                .filter(c -> c.isoCode().equalsIgnoreCase(isoCode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Country not found: " + isoCode));

        return soapClient.countriesUsingCurrency(country.currencyIsoCode()).stream()
                .filter(c -> !c.isoCode().equalsIgnoreCase(isoCode))
                .map(c -> CountrySummaryDto.builder().isoCode(c.isoCode()).name(c.name()).build())
                .collect(Collectors.toList());
    }

    @Cacheable(value = CacheConfig.CACHE_FULL_COUNTRIES)
    public List<FullCountryResult> getAllCountries() {
        return soapClient.fullCountryInfoAllCountries();
    }

    public void warmCache() {
        log.info("Starting cache warm-up...");
        try {
            getContinents();
            log.info("Continents cache warmed");
        } catch (Exception e) {
            log.warn("Failed to warm continents cache", e);
        }
        try {
            getCurrencies();
            log.info("Currencies cache warmed");
        } catch (Exception e) {
            log.warn("Failed to warm currencies cache", e);
        }
        try {
            getLanguages();
            log.info("Languages cache warmed");
        } catch (Exception e) {
            log.warn("Failed to warm languages cache", e);
        }
        try {
            getAllCountries();
            log.info("Full countries cache warmed");
        } catch (Exception e) {
            log.warn("Failed to warm full countries cache", e);
        }
        try {
            soapClient.listCountryNamesGroupedByContinent();
            log.info("Countries grouped by continent cache warmed");
        } catch (Exception e) {
            log.warn("Failed to warm countries grouped cache", e);
        }
        log.info("Cache warm-up complete");
    }

    private void ensureLookupMapsPopulated() {
        if (continentCodeToName.isEmpty()) {
            try { getContinents(); } catch (Exception ignored) {}
        }
        if (currencyCodeToName.isEmpty()) {
            try { getCurrencies(); } catch (Exception ignored) {}
        }
        if (languageCodeToName.isEmpty()) {
            try { getLanguages(); } catch (Exception ignored) {}
        }
    }

    private CountryResponse toCountryResponse(FullCountryResult c) {
        return CountryResponse.builder()
                .isoCode(c.isoCode())
                .name(c.name())
                .capitalCity(c.capitalCity())
                .phoneCode(c.phoneCode())
                .continentCode(c.continentCode())
                .continentName(continentCodeToName.getOrDefault(c.continentCode(), c.continentCode()))
                .currencyIsoCode(c.currencyIsoCode())
                .currencyName(currencyCodeToName.getOrDefault(c.currencyIsoCode(), c.currencyIsoCode()))
                .countryFlag(c.countryFlag())
                .languages(c.languages().stream()
                        .map(l -> LanguageDto.builder()
                                .isoCode(l.isoCode())
                                .name(languageCodeToName.getOrDefault(l.isoCode(), l.name()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private boolean hasLanguage(FullCountryResult country, String languageCode) {
        return country.languages().stream()
                .anyMatch(l -> l.isoCode().equalsIgnoreCase(languageCode));
    }

    private List<CountryResponse> applySort(List<CountryResponse> list, String sort) {
        if (sort == null || sort.isBlank()) return list;

        Comparator<CountryResponse> comparator = null;
        String[] parts = sort.split(",");
        for (String part : parts) {
            String[] fieldDir = part.trim().split(":");
            String field = fieldDir[0].trim();
            boolean asc = fieldDir.length < 2 || !"desc".equalsIgnoreCase(fieldDir[1].trim());

            Comparator<CountryResponse> fieldComparator = switch (field.toLowerCase()) {
                case "name" -> Comparator.comparing(CountryResponse::getName, String.CASE_INSENSITIVE_ORDER);
                case "isocode" -> Comparator.comparing(CountryResponse::getIsoCode);
                case "capitalcity" -> Comparator.comparing(CountryResponse::getCapitalCity, String.CASE_INSENSITIVE_ORDER);
                case "continentcode" -> Comparator.comparing(CountryResponse::getContinentCode);
                case "continentname" -> Comparator.comparing(CountryResponse::getContinentName, String.CASE_INSENSITIVE_ORDER);
                default -> null;
            };

            if (fieldComparator != null) {
                if (!asc) fieldComparator = fieldComparator.reversed();
                comparator = comparator == null ? fieldComparator : comparator.thenComparing(fieldComparator);
            }
        }
        if (comparator != null) {
            list.sort(comparator);
        }
        return list;
    }
}
