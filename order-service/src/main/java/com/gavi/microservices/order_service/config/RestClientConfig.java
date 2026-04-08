package com.gavi.microservices.order_service.config;

import com.gavi.microservices.order_service.client.InventoryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;


@Configuration
@Slf4j
public class RestClientConfig {


    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Bean
    public InventoryClient inventoryClient() {
        log.info("Creating InventoryClient with base URL: {}", inventoryServiceUrl);
        RestClient restClient = RestClient.builder()
                .baseUrl(inventoryServiceUrl)
                .requestFactory(getClientRequestFactory())
                .defaultStatusHandler(status -> status.is4xxClientError() || status.is5xxServerError(), 
                    (request, response) -> {
                        log.error("Inventory service returned status: {} for request: {}", 
                                response.getStatusCode(), request.getURI());
                        throw new org.springframework.web.client.HttpServerErrorException(
                            response.getStatusCode(), "Inventory service error: " + response.getStatusText());
                    })
                .build();
        var restClientAdapter = RestClientAdapter.create(restClient);
        var httpServiceProxyFactory = HttpServiceProxyFactory.builderFor(restClientAdapter).build();
        return httpServiceProxyFactory.createClient(InventoryClient.class);
    }

    private ClientHttpRequestFactory getClientRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 3 seconds
        factory.setReadTimeout(3000); // 3 seconds
        
        log.info("Configured RestClient with connect timeout: 3000ms, read timeout: 3000ms");
        
        return factory;
    }

}
