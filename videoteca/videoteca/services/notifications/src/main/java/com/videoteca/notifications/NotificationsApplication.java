package com.videoteca.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio de notificaciones.
 *
 * <p>El servicio ENVÍA notificaciones salientes por email (SMTP) y RECIBE
 * notificaciones entrantes de otros servicios. La persistencia es EN MEMORIA
 * (ConcurrentHashMap): los datos se pierden al reiniciar la aplicación.</p>
 */
@SpringBootApplication
public class NotificationsApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationsApplication.class, args);
	}

}
