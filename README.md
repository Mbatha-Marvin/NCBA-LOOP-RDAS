# Reference Data Aggregation Service (RDAS)

## Overview

RDAS provides a REST/JSON API that wraps the CountryInfo SOAP service internally, solving the operational challenges of multiple channels consuming SOAP directly.

### Features

- Search countries by name, continent, currency, or language
- In-memory caching with Caffeine (24h for reference data, 6-12h for country data)
- Circuit breaker pattern via Resilience4j for SOAP failure handling
- Pagination and multi-field sorting
- Input validation with Jakarta Bean Validation
- Global error handling with structured error responses
- OpenAPI/Swagger UI documentation
- Prometheus metrics via Micrometer
- Kubernetes-ready with liveness/readiness probes

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+

### Run Locally

```bash
mvn clean spring-boot:run
```

The application starts on port 8080.

### Swagger UI

http://localhost:8080/swagger-ui.html

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/countries` | Search, filter, paginate, sort countries |
| GET | `/api/v1/countries/{isoCode}` | Country detail with sharing-currency info |
| GET | `/api/v1/countries/{isoCode}/sharing-currency` | Countries sharing same currency |
| GET | `/api/v1/continents` | List all continents |
| GET | `/api/v1/currencies` | List all currencies |
| GET | `/api/v1/languages` | List all languages |

### Example Requests

```bash
# Search countries
curl "http://localhost:8080/api/v1/countries?name=India&page=0&size=10&sort=name:asc"

# Search by continent and currency
curl "http://localhost:8080/api/v1/countries?continent=EU&currency=EUR&sort=name:asc"

# Get country detail
curl "http://localhost:8080/api/v1/countries/US"

# List continents
curl "http://localhost:8080/api/v1/continents"
```

## Caching Strategy

| Data | Cache TTL | Justification |
|------|-----------|---------------|
| Continents | 24 hours | Static reference data |
| Currencies | 24 hours | Static reference data |
| Languages | 24 hours | Static reference data |
| Full country list | 12 hours | Countries rarely change |
| Country details | 6 hours | Per-country cache |
| Currency sharing | 6 hours | Per-currency cache |

Cache is warmed asynchronously on application startup to ensure fast first requests.

## Resilience

- **Circuit Breaker**: Opens at 50% failure rate (5+ calls), half-open after 30s
- **Fallback**: Returns stale cached data when available
- **SOAP Unavailable**: Returns 503 with a user-friendly message if no cache exists

## Monitoring

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

## Technology Stack

- Java 17
- Spring Boot 3.3
- Spring Cache with Caffeine
- Resilience4j Circuit Breaker
- SpringDoc OpenAPI
- Micrometer + Prometheus
