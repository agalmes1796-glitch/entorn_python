package com.videoteca.frontend;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Bean de {@link RestClient} per consultar el gateway.
 *
 * <p>El <i>base URL</i> ve de la propietat {@code gateway.base-url}
 * (per defecte {@code http://gateway:8080}; sobrescribible amb la variable
 * d'entorn {@code GATEWAY_BASE_URL}). El timeout de connexió i de lectura de
 * 5 s (JDK {@link HttpClient} + {@link JdkClientHttpRequestFactory}) fa que,
 * si el gateway no respon en aquest termini, la petició falli ràpidament i el
 * controlador pugui respondre 503 (sense que s'acumulin peticions pendents).</p>
 */
@Configuration
class RestClientConfig {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Bean
    RestClient restClient(@Value("${gateway.base-url}") String gatewayBaseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(TIMEOUT);
        return RestClient.builder()
                .baseUrl(gatewayBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
