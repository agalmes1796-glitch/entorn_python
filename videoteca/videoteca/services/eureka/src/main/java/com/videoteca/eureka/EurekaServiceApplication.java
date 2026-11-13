package com.videoteca.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Punto de entrada de eureka: servidor de registro de servicios
 * (Spring Cloud Netflix Eureka) para el proyecto videoteca.
 *
 * {@link EnableEurekaServer} es obligatorio en el train Spring Cloud 2025.1 (SCN 5.0.x):
 * registra el marker bean que habilita EurekaServerAutoConfiguration y el transporte
 * de cliente (sin él, el arranque falla al crear el bean eurekaClient).
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServiceApplication.class, args);
    }

}
