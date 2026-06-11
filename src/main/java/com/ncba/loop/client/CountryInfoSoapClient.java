package com.ncba.loop.client;

import com.ncba.loop.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CountryInfoSoapClient {

    private static final Logger log = LoggerFactory.getLogger(CountryInfoSoapClient.class);
    private static final String NAMESPACE = "http://www.oorsprong.org/websamples.countryinfo";
    private static final String SOAP_ACTION_PREFIX = "http://www.oorsprong.org/websamples.countryinfo/";

    private final RestTemplate restTemplate;
    private final String soapEndpoint;
    private final DocumentBuilder documentBuilder;
    private final XPath xPath;

    private final AtomicReference<List<ContinentResult>> cachedContinents = new AtomicReference<>();
    private final AtomicReference<List<ContinentResult>> cachedContinentsFallback = new AtomicReference<>();
    private final AtomicReference<List<CountryGroupedResult>> cachedCountriesGrouped = new AtomicReference<>();
    private final AtomicReference<List<CountryGroupedResult>> cachedCountriesGroupedFallback = new AtomicReference<>();
    private final AtomicReference<List<FullCountryResult>> cachedFullCountries = new AtomicReference<>();
    private final AtomicReference<List<FullCountryResult>> cachedFullCountriesFallback = new AtomicReference<>();
    private final AtomicReference<List<CurrencyResult>> cachedCurrencies = new AtomicReference<>();
    private final AtomicReference<List<CurrencyResult>> cachedCurrenciesFallback = new AtomicReference<>();
    private final AtomicReference<List<LanguageResult>> cachedLanguages = new AtomicReference<>();
    private final AtomicReference<List<LanguageResult>> cachedLanguagesFallback = new AtomicReference<>();
    private final ConcurrentHashMap<String, List<CountryNameResult>> cachedCountriesByCurrency = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<CountryNameResult>> cachedCountriesByCurrencyFallback = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FullCountryResult> cachedCountryDetails = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FullCountryResult> cachedCountryDetailsFallback = new ConcurrentHashMap<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public CountryInfoSoapClient(@Qualifier("soapRestTemplate") RestTemplate restTemplate,
                                  @Qualifier("soapEndpoint") String soapEndpoint) {
        this.restTemplate = restTemplate;
        this.soapEndpoint = soapEndpoint;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            this.documentBuilder = factory.newDocumentBuilder();
            this.xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    return "ns".equals(prefix) ? NAMESPACE : javax.xml.XMLConstants.NULL_NS_URI;
                }
                @Override
                public String getPrefix(String namespaceURI) {
                    return NAMESPACE.equals(namespaceURI) ? "ns" : null;
                }
                @Override
                public java.util.Iterator<String> getPrefixes(String namespaceURI) {
                    return List.of("ns").iterator();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize XML parser", e);
        }
    }

    @CircuitBreaker(name = "countryInfoSoap", fallbackMethod = "listContinentsFallback")
    public List<ContinentResult> listContinentsByName() {
        try {
            String soapBody = buildListRequest("ListOfContinentsByName");
            String response = sendSoapRequest(soapBody, "ListOfContinentsByName");
            List<ContinentResult> results = parseContinentList(response);
            cachedContinents.set(results);
            cachedContinentsFallback.set(results);
            return results;
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to list continents", e);
            throw new ServiceUnavailableException("Failed to retrieve continents from SOAP service", e);
        }
    }

    @CircuitBreaker(name = "countryInfoSoap", fallbackMethod = "listCurrenciesFallback")
    public List<CurrencyResult> listCurrenciesByName() {
        try {
            String soapBody = buildListRequest("ListOfCurrenciesByName");
            String response = sendSoapRequest(soapBody, "ListOfCurrenciesByName");
            List<CurrencyResult> results = parseCurrencyList(response);
            cachedCurrencies.set(results);
            cachedCurrenciesFallback.set(results);
            return results;
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to list currencies", e);
            throw new ServiceUnavailableException("Failed to retrieve currencies from SOAP service", e);
        }
    }

    @CircuitBreaker(name = "countryInfoSoap", fallbackMethod = "listLanguagesFallback")
    public List<LanguageResult> listLanguagesByName() {
        try {
            String soapBody = buildListRequest("ListOfLanguagesByName");
            String response = sendSoapRequest(soapBody, "ListOfLanguagesByName");
            List<LanguageResult> results = parseLanguageList(response);
            cachedLanguages.set(results);
            cachedLanguagesFallback.set(results);
            return results;
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to list languages", e);
            throw new ServiceUnavailableException("Failed to retrieve languages from SOAP service", e);
        }
    }

    @CircuitBreaker(name = "countryInfoSoap", fallbackMethod = "listCountriesGroupedFallback")
    public List<CountryGroupedResult> listCountryNamesGroupedByContinent() {
        try {
            String soapBody = buildListRequest("ListOfCountryNamesGroupedByContinent");
            String response = sendSoapRequest(soapBody, "ListOfCountryNamesGroupedByContinent");
            List<CountryGroupedResult> results = parseCountriesGrouped(response);
            cachedCountriesGrouped.set(results);
            cachedCountriesGroupedFallback.set(results);
            return results;
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to list countries grouped by continent", e);
            throw new ServiceUnavailableException("Failed to retrieve countries grouped by continent from SOAP service", e);
        }
    }

    @CircuitBreaker(name = "countryInfoSoap", fallbackMethod = "fullCountryInfoAllCountriesFallback")
    public List<FullCountryResult> fullCountryInfoAllCountries() {
        try {
            String soapBody = buildListRequest("FullCountryInfoAllCountries");
            String response = sendSoapRequest(soapBody, "FullCountryInfoAllCountries");
            List<FullCountryResult> results = parseFullCountryList(response);
            cachedFullCountries.set(results);
            cachedFullCountriesFallback.set(results);
            for (FullCountryResult fc : results) {
                cachedCountryDetails.put(fc.isoCode, fc);
                cachedCountryDetailsFallback.put(fc.isoCode, fc);
            }
            return results;
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get all countries full info", e);
            throw new ServiceUnavailableException("Failed to retrieve full country info from SOAP service", e);
        }
    }

    @CircuitBreaker(name = "countryInfoSoap", fallbackMethod = "countriesUsingCurrencyFallback")
    public List<CountryNameResult> countriesUsingCurrency(String currencyIsoCode) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            sb.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
            sb.append("<soap:Body>");
            sb.append("<CountriesUsingCurrency xmlns=\"").append(NAMESPACE).append("\">");
            sb.append("<sISOCurrencyCode>").append(escapeXml(currencyIsoCode)).append("</sISOCurrencyCode>");
            sb.append("</CountriesUsingCurrency>");
            sb.append("</soap:Body>");
            sb.append("</soap:Envelope>");
            String response = sendSoapRequest(sb.toString(), "CountriesUsingCurrency");
            List<CountryNameResult> results = parseCountryNameList(response);
            cachedCountriesByCurrency.put(currencyIsoCode, results);
            cachedCountriesByCurrencyFallback.put(currencyIsoCode, results);
            return results;
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get countries using currency: {}", currencyIsoCode, e);
            throw new ServiceUnavailableException("Failed to retrieve countries using currency from SOAP service", e);
        }
    }

    private String buildListRequest(String operation) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                "<" + operation + " xmlns=\"" + NAMESPACE + "\"/>" +
                "</soap:Body>" +
                "</soap:Envelope>";
    }

    private String sendSoapRequest(String soapBody, String operation) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.set("SOAPAction", SOAP_ACTION_PREFIX + operation);

        HttpEntity<String> request = new HttpEntity<>(soapBody, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(soapEndpoint, request, String.class);
            if (response.getBody() == null) {
                throw new ServiceUnavailableException("Empty response from SOAP service");
            }
            return response.getBody();
        } catch (ResourceAccessException e) {
            log.error("Connection to SOAP service failed", e);
            throw new ServiceUnavailableException("Unable to connect to SOAP service: " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.error("SOAP request failed for operation: {}", operation, e);
            throw new ServiceUnavailableException("SOAP request failed for " + operation + ": " + e.getMessage(), e);
        }
    }

    private List<ContinentResult> parseContinentList(String soapResponse) throws Exception {
        return parseList(soapResponse, "//ns:tContinent",
                node -> new ContinentResult(
                        getChildText(node, "ns:sCode"),
                        getChildText(node, "ns:sName")
                ));
    }

    private List<CurrencyResult> parseCurrencyList(String soapResponse) throws Exception {
        return parseList(soapResponse, "//ns:tCurrency",
                node -> new CurrencyResult(
                        getChildText(node, "ns:sISOCode"),
                        getChildText(node, "ns:sName")
                ));
    }

    private List<LanguageResult> parseLanguageList(String soapResponse) throws Exception {
        return parseList(soapResponse, "//ns:tLanguage",
                node -> new LanguageResult(
                        getChildText(node, "ns:sISOCode"),
                        getChildText(node, "ns:sName")
                ));
    }

    private List<CountryGroupedResult> parseCountriesGrouped(String soapResponse) throws Exception {
        List<CountryGroupedResult> results = new ArrayList<>();
        Document doc = documentBuilder.parse(new ByteArrayInputStream(soapResponse.getBytes(StandardCharsets.UTF_8)));
        NodeList groupedNodes = (NodeList) xPath.evaluate("//ns:tCountryCodeAndNameGroupedByContinent", doc, XPathConstants.NODESET);
        for (int i = 0; i < groupedNodes.getLength(); i++) {
            Element groupElem = (Element) groupedNodes.item(i);
            ContinentResult continent = null;
            NodeList continentNodes = groupElem.getElementsByTagNameNS(NAMESPACE, "Continent");
            if (continentNodes.getLength() > 0) {
                Element continentElem = (Element) continentNodes.item(0);
                continent = new ContinentResult(
                        getChildText(continentElem, "ns:sCode"),
                        getChildText(continentElem, "ns:sName")
                );
            }
            List<CountryNameResult> countries = new ArrayList<>();
            NodeList countryNodes = groupElem.getElementsByTagNameNS(NAMESPACE, "tCountryCodeAndName");
            for (int j = 0; j < countryNodes.getLength(); j++) {
                Element countryElem = (Element) countryNodes.item(j);
                countries.add(new CountryNameResult(
                        getChildText(countryElem, "ns:sISOCode"),
                        getChildText(countryElem, "ns:sName")
                ));
            }
            results.add(new CountryGroupedResult(continent, countries));
        }
        return results;
    }

    private List<FullCountryResult> parseFullCountryList(String soapResponse) throws Exception {
        List<FullCountryResult> results = new ArrayList<>();
        Document doc = documentBuilder.parse(new ByteArrayInputStream(soapResponse.getBytes(StandardCharsets.UTF_8)));
        NodeList countryNodes = (NodeList) xPath.evaluate("//ns:tCountryInfo", doc, XPathConstants.NODESET);
        for (int i = 0; i < countryNodes.getLength(); i++) {
            Element elem = (Element) countryNodes.item(i);
            String isoCode = getChildText(elem, "ns:sISOCode");
            String name = getChildText(elem, "ns:sName");
            String capitalCity = getChildText(elem, "ns:sCapitalCity");
            String phoneCode = getChildText(elem, "ns:sPhoneCode");
            String continentCode = getChildText(elem, "ns:sContinentCode");
            String currencyIsoCode = getChildText(elem, "ns:sCurrencyISOCode");
            String countryFlag = getChildText(elem, "ns:sCountryFlag");

            List<LanguageResult> languages = new ArrayList<>();
            NodeList langNodes = elem.getElementsByTagNameNS(NAMESPACE, "tLanguage");
            for (int j = 0; j < langNodes.getLength(); j++) {
                Element langElem = (Element) langNodes.item(j);
                languages.add(new LanguageResult(
                        getChildText(langElem, "ns:sISOCode"),
                        getChildText(langElem, "ns:sName")
                ));
            }

            results.add(new FullCountryResult(isoCode, name, capitalCity, phoneCode, continentCode, currencyIsoCode, countryFlag, languages));
        }
        return results;
    }

    private List<CountryNameResult> parseCountryNameList(String soapResponse) throws Exception {
        return parseList(soapResponse, "//ns:tCountryCodeAndName",
                node -> new CountryNameResult(
                        getChildText(node, "ns:sISOCode"),
                        getChildText(node, "ns:sName")
                ));
    }

    @FunctionalInterface
    private interface NodeParser<T> {
        T parse(Node node);
    }

    private <T> List<T> parseList(String soapResponse, String expression, NodeParser<T> parser) throws Exception {
        List<T> results = new ArrayList<>();
        Document doc = documentBuilder.parse(new ByteArrayInputStream(soapResponse.getBytes(StandardCharsets.UTF_8)));
        NodeList nodeList = (NodeList) xPath.evaluate(expression, doc, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            results.add(parser.parse(nodeList.item(i)));
        }
        return results;
    }

    private String getChildText(Node context, String childXPath) {
        try {
            Node node = (Node) xPath.evaluate(childXPath, context, XPathConstants.NODE);
            return node != null ? node.getTextContent() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // Fallback methods
    public List<ContinentResult> listContinentsFallback(Exception e) {
        log.warn("Circuit breaker active - returning cached continents");
        List<ContinentResult> cached = cachedContinentsFallback.get();
        if (cached != null) return cached;
        throw new ServiceUnavailableException("SOAP service unavailable and no cached continents data available", e);
    }

    public List<CurrencyResult> listCurrenciesFallback(Exception e) {
        log.warn("Circuit breaker active - returning cached currencies");
        List<CurrencyResult> cached = cachedCurrenciesFallback.get();
        if (cached != null) return cached;
        throw new ServiceUnavailableException("SOAP service unavailable and no cached currencies data available", e);
    }

    public List<LanguageResult> listLanguagesFallback(Exception e) {
        log.warn("Circuit breaker active - returning cached languages");
        List<LanguageResult> cached = cachedLanguagesFallback.get();
        if (cached != null) return cached;
        throw new ServiceUnavailableException("SOAP service unavailable and no cached languages data available", e);
    }

    public List<CountryGroupedResult> listCountriesGroupedFallback(Exception e) {
        log.warn("Circuit breaker active - returning cached countries grouped");
        List<CountryGroupedResult> cached = cachedCountriesGroupedFallback.get();
        if (cached != null) return cached;
        throw new ServiceUnavailableException("SOAP service unavailable and no cached countries data available", e);
    }

    public List<FullCountryResult> fullCountryInfoAllCountriesFallback(Exception e) {
        log.warn("Circuit breaker active - returning cached full countries");
        List<FullCountryResult> cached = cachedFullCountriesFallback.get();
        if (cached != null) return cached;
        throw new ServiceUnavailableException("SOAP service unavailable and no cached full country data available", e);
    }

    public List<CountryNameResult> countriesUsingCurrencyFallback(String currencyIsoCode, Exception e) {
        log.warn("Circuit breaker active - returning cached countries for currency: {}", currencyIsoCode);
        List<CountryNameResult> cached = cachedCountriesByCurrencyFallback.get(currencyIsoCode);
        if (cached != null) return cached;
        throw new ServiceUnavailableException("SOAP service unavailable and no cached data for currency: " + currencyIsoCode, e);
    }

    // Result types
    public record ContinentResult(String code, String name) {}
    public record CurrencyResult(String isoCode, String name) {}
    public record LanguageResult(String isoCode, String name) {}
    public record CountryNameResult(String isoCode, String name) {}
    public record CountryGroupedResult(ContinentResult continent, List<CountryNameResult> countries) {}
    public record FullCountryResult(String isoCode, String name, String capitalCity, String phoneCode,
                                     String continentCode, String currencyIsoCode, String countryFlag,
                                     List<LanguageResult> languages) {}
}
