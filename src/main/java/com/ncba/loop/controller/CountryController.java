package com.ncba.loop.controller;

import com.ncba.loop.dto.*;
import com.ncba.loop.service.CountryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/countries")
@Tag(name = "Countries", description = "Country search, detail, and sharing-currency operations")
@Validated
public class CountryController {

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping
    @Operation(summary = "Search and filter countries", description = "Search countries by name, filter by continent/currency/language, with pagination and sorting")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful search"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "503", description = "SOAP service unavailable")
    })
    public ResponseEntity<PagedResponse<CountryResponse>> searchCountries(
            @Parameter(description = "Country name (partial match)") @RequestParam(required = false) String name,
            @Parameter(description = "Continent code filter") @RequestParam(required = false) String continent,
            @Parameter(description = "Currency ISO code filter") @RequestParam(required = false) String currency,
            @Parameter(description = "Language ISO code filter") @RequestParam(required = false) String language,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field(s): name, isoCode, capitalCity, continentCode, continentName. Use :asc or :desc", schema = @Schema(example = "name:asc")) @RequestParam(defaultValue = "name:asc") String sort) {
        return ResponseEntity.ok(countryService.searchCountries(name, continent, currency, language, page, size, sort));
    }

    @GetMapping("/{isoCode}")
    @Operation(summary = "Get country detail", description = "Retrieve full details for a country by ISO code, including countries sharing the same currency")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Country found"),
            @ApiResponse(responseCode = "404", description = "Country not found"),
            @ApiResponse(responseCode = "503", description = "SOAP service unavailable")
    })
    public ResponseEntity<CountryDetailResponse> getCountryDetail(
            @Parameter(description = "ISO country code (e.g., US, GB, KE)") @PathVariable String isoCode) {
        return ResponseEntity.ok(countryService.getCountryDetail(isoCode));
    }

    @GetMapping("/{isoCode}/sharing-currency")
    @Operation(summary = "Countries sharing currency", description = "Find all countries that share the same currency as the given country")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful"),
            @ApiResponse(responseCode = "404", description = "Country not found"),
            @ApiResponse(responseCode = "503", description = "SOAP service unavailable")
    })
    public ResponseEntity<java.util.List<CountrySummaryDto>> getCountriesSharingCurrency(
            @Parameter(description = "ISO country code (e.g., US, GB, KE)") @PathVariable String isoCode) {
        return ResponseEntity.ok(countryService.getCountriesSharingCurrency(isoCode));
    }
}
