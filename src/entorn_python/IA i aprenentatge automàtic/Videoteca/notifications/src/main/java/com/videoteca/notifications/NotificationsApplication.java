package com.videoteca.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservici de notificacions del projecte Videoteca.
 *
 * <p>Gestiona el cicle de vida de les notificacions (alta, consulta,
 * modificació, baixa i marques de lectura) i les persisteix a MariaDB
 * (taula {@code notifications}); es registra al servidor Eureka perquè
 * el gateway el pugui descobrir (vegeu {@code application.yml}).</p>
 */
@SpringBootApplication
public class NotificationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationsApplication.class, args);
    }
}
