package com.videoteca.frontend.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Client HTTP per parlar amb l'API Gateway.
 *
 * <p>{@code gateway.base-url} defineix la base (per defecte
 * {@code http://gateway:8080}, dins de la xarxa interna de Docker); es pot
 * sobrescriure amb la variable d'entorn {@code GATEWAY_BASE_URL} (per exemple
 * {@code http://localhost:8081} en desenvolupament local). El timeout de
 * connexió i de lectura és de 5 s per no aturar les peticions si el gateway
 respon lent o no està disponible.</p>
 */
@Configuration
public class GatewayClientConfig {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient gatewayRestClient(@Value("${gateway.base-url}") String gatewayBaseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return RestClient.builder()
                .baseUrl(gatewayBaseUrl)
                .requestFactory(factory)
                .build();
    }
}
