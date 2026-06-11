package com.ncba.loop.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String CACHE_CONTINENTS = "continents";
    public static final String CACHE_CURRENCIES = "currencies";
    public static final String CACHE_LANGUAGES = "languages";
    public static final String CACHE_COUNTRIES_GROUPED = "countriesGrouped";
    public static final String CACHE_COUNTRY_DETAILS = "countryDetails";
    public static final String CACHE_COUNTRIES_USING_CURRENCY = "countriesUsingCurrency";
    public static final String CACHE_FULL_COUNTRIES = "fullCountries";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .recordStats());

        cacheManager.registerCustomCache(CACHE_CONTINENTS,
                Caffeine.newBuilder().maximumSize(1).expireAfterWrite(24, TimeUnit.HOURS).recordStats().build());
        cacheManager.registerCustomCache(CACHE_CURRENCIES,
                Caffeine.newBuilder().maximumSize(1).expireAfterWrite(24, TimeUnit.HOURS).recordStats().build());
        cacheManager.registerCustomCache(CACHE_LANGUAGES,
                Caffeine.newBuilder().maximumSize(1).expireAfterWrite(24, TimeUnit.HOURS).recordStats().build());
        cacheManager.registerCustomCache(CACHE_COUNTRIES_GROUPED,
                Caffeine.newBuilder().maximumSize(1).expireAfterWrite(12, TimeUnit.HOURS).recordStats().build());
        cacheManager.registerCustomCache(CACHE_COUNTRY_DETAILS,
                Caffeine.newBuilder().maximumSize(300).expireAfterWrite(6, TimeUnit.HOURS).recordStats().build());
        cacheManager.registerCustomCache(CACHE_COUNTRIES_USING_CURRENCY,
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(6, TimeUnit.HOURS).recordStats().build());
        cacheManager.registerCustomCache(CACHE_FULL_COUNTRIES,
                Caffeine.newBuilder().maximumSize(1).expireAfterWrite(12, TimeUnit.HOURS).recordStats().build());

        return cacheManager;
    }
}
