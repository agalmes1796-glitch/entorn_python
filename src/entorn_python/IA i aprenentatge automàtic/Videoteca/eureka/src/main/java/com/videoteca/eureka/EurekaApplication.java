package com.videoteca.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor Eureka del projecte Videoteca: registre de serveis.
 *
 * <p>No es registra ni consulta cap altre directori (és el directori
 * d'origen), ve configurat a {@code application.yml}.</p>
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaApplication.class, args);
    }
}
