package com.ncba.loop.cache;

import com.ncba.loop.service.CountryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class CacheWarmer {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmer.class);

    private final CountryService countryService;

    public CacheWarmer(CountryService countryService) {
        this.countryService = countryService;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpOnStartup() {
        log.info("Triggering async cache warm-up on application ready...");
        countryService.warmCache();
    }
}
