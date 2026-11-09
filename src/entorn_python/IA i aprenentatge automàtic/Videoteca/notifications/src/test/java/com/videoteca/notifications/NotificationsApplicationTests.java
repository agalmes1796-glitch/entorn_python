package com.videoteca.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Càrrega del context del microservici de notificacions.
 *
 * <p>La configuració de prova (H2 en memòria amb {@code MODE=MYSQL} i
 * Eureka desactivat) ve del {@code src/test/resources/application.yml}:
 * el test no depèn d'una MariaDB ni d'un servidor Eureka reals.</p>
 */
@SpringBootTest
class NotificationsApplicationTests {

    @Test
    void contextLoads() {
    }
}
