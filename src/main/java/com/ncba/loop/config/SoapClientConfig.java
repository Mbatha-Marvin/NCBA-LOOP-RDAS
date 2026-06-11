package com.ncba.loop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SoapClientConfig {

    @Value("${soap.country-info.endpoint}")
    private String soapEndpoint;

    @Value("${soap.country-info.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${soap.country-info.read-timeout:10000}")
    private int readTimeout;

    @Bean
    public RestTemplate soapRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }

    @Bean
    public String soapEndpoint() {
        return soapEndpoint;
    }
}
