package com.videoteca.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de gateway: API gateway reactivo
 * (Spring Cloud Gateway sobre WebFlux) para el proyecto videoteca.
 * El starter de gateway incorpora WebFlux: no hay que incluir
 * spring-boot-starter-web (MVC) en este proyecto.
 */
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

}
