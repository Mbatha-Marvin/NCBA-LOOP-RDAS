# Architecture

## System Context

```
┌──────────────┐  ┌──────────┐  ┌─────────────┐  ┌──────────────┐
│   Mobile     │  │   Web    │  │ Partner API │  │   Internal   │
│     App      │  │   App    │  │  Consumers  │  │   Portals    │
└──────┬───────┘  └────┬─────┘  └──────┬──────┘  └──────┬───────┘
       │               │              │                │
       └───────────────┴──────────────┴────────────────┘
                           │ REST/JSON
                           ▼
              ┌─────────────────────────┐
              │         RDAS            │
              │  (Spring Boot REST API)  │
              └────────────┬────────────┘
                           │ SOAP/XML
                           ▼
              ┌─────────────────────────┐
              │  CountryInfo SOAP       │
              │  (External Service)     │
              └─────────────────────────┘
```

## Internal Architecture

```
┌──────────────────────────────────────────────────────┐
│                   REST Controllers                    │
│  CountryController   ReferenceDataController          │
│       │                       │                       │
│       └───────────┬───────────┘                       │
│                   ▼                                   │
│             CountryService                            │
│        (Orchestration, Filtering,                     │
│         Sorting, Aggregation)                         │
│       │                    │                          │
│       ▼                    ▼                          │
│  Spring Cache        CountryInfoSoapClient            │
│  (Caffeine)          (SOAP + Circuit Breaker)         │
│       │                    │                          │
│       │                    ▼                          │
│       │         Resilience4j CircuitBreaker            │
│       │         ├─ Primary: SOAP call                  │
│       │         └─ Fallback: Cached data               │
│       ▼                                               │
│  In-Memory Cache                                      │
│  (Caffeine: maxSize=500,                               │
│   TTL=6h-24h depending on data)                       │
└──────────────────────────────────────────────────────┘
```

## Component Details

### REST Controllers
- `CountryController` — Search, detail, sharing-currency endpoints
- `ReferenceDataController` — Continents, currencies, languages endpoints
- Input validation via `@Validated` + Jakarta Bean Validation annotations

### Service Layer
- `CountryService` — In-memory filtering, sorting, pagination from cached full country dataset
- All reference data resolutions (continent name, currency name, language name) from lookup maps

### SOAP Client
- Raw SOAP/XML envelope construction and response parsing
- XPath-based XML extraction
- Resilience4j `@CircuitBreaker` on every SOAP call

### Caching
- Caffeine via Spring Cache abstraction
- Dedicated cache regions with appropriate TTLs
- Async cache warm-up on `ApplicationReadyEvent`

### Error Handling
- `GlobalExceptionHandler` — `@RestControllerAdvice` catching all exceptions
- Structured JSON error responses with status, path, timestamp
- Validation errors returned per-field

## Data Flow

### Search Countries
1. Request arrives at `GET /api/v1/countries`
2. Controller validates query parameters
3. `CountryService` loads full country list from cache (or SOAP if cold)
4. In-memory filtering by name/continent/currency/language
5. In-memory sorting
6. In-memory pagination
7. Returns `PagedResponse<CountryResponse>`

### Country Detail
1. Request arrives at `GET /api/v1/countries/{isoCode}`
2. `CountryService` loads full country list from cache
3. Finds country by ISO code (404 if not found)
4. Calls `CountriesUsingCurrency` SOAP operation
5. Builds `CountryDetailResponse` with sharing-currency list
6. Returns response

## Deploy

```
┌──────────────────────────────────────────────┐
│              k3d cluster: ncba-loop          │
│                                              │
│  ┌────────────────────────────────────┐      │
│  │  Namespace: ncba-loop               │      │
│  │                                     │      │
│  │  ┌───────────┐   ┌──────────────┐  │      │
│  │  │  Service   │──▶│  Deployment   │  │      │
│  │  │  :8080     │   │  1 replica    │  │      │
│  │  │  ClusterIP │   │  rdas:1.0.0   │  │      │
│  │  └───────────┘   └──────┬───────┘  │      │
│  │                         │           │      │
│  │                    ┌────▼──────┐    │      │
│  │                    │ ConfigMap  │    │      │
│  │                    └───────────┘    │      │
│  └────────────────────────────────────┘      │
└──────────────────────────────────────────────┘
```

## SOAP Call Budget

Under the 100 requests/minute limit:

| Phase | SOAP Calls | When |
|-------|-----------|------|
| Cache warm-up | 4 | Startup only |
| FullCountryInfoAllCountries | 1 per 12h | Cache refresh |
| CountriesUsingCurrency | 0-N per 6h | Per unique currency lookup |
| Total typical | ~5/min steady state | Well within limit |
