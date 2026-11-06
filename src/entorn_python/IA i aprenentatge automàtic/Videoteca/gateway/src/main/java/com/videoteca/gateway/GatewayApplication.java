package com.videoteca.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway del projecte Videoteca.
 *
 * <p>Porta d'entrada de les peticions; redirigeix les cridades als
 * serveis registrats al servidor Eureka gràcies al discovery locator
 * (vegeu {@code application.yml}).</p>
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
