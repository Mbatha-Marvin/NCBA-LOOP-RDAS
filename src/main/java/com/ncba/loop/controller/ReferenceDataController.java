package com.ncba.loop.controller;

import com.ncba.loop.dto.ContinentDto;
import com.ncba.loop.dto.CurrencyDto;
import com.ncba.loop.dto.LanguageDto;
import com.ncba.loop.service.CountryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reference Data", description = "Continent, currency, and language reference data")
public class ReferenceDataController {

    private final CountryService countryService;

    public ReferenceDataController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping("/continents")
    @Operation(summary = "List all continents")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful"),
            @ApiResponse(responseCode = "503", description = "SOAP service unavailable")
    })
    public ResponseEntity<List<ContinentDto>> getContinents() {
        return ResponseEntity.ok(countryService.getContinents());
    }

    @GetMapping("/currencies")
    @Operation(summary = "List all currencies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful"),
            @ApiResponse(responseCode = "503", description = "SOAP service unavailable")
    })
    public ResponseEntity<List<CurrencyDto>> getCurrencies() {
        return ResponseEntity.ok(countryService.getCurrencies());
    }

    @GetMapping("/languages")
    @Operation(summary = "List all languages")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful"),
            @ApiResponse(responseCode = "503", description = "SOAP service unavailable")
    })
    public ResponseEntity<List<LanguageDto>> getLanguages() {
        return ResponseEntity.ok(countryService.getLanguages());
    }
}
