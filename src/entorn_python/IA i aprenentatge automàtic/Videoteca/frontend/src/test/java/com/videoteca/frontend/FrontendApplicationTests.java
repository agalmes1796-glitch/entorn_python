package com.videoteca.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Càrrega del context del microservici de front.
 *
 * <p>{@code eureka.client.enabled=false} desactiva el client de Eureka en el
 * test: de manera determinista i ràpida, sense reintents en background contra
 * {@code localhost:8761} (inexistent en l'entorn de test). El que s'assegura
 * amb aquest test és que el context s'obri (inclos el bean de {@code RestClient}
 * amb els timeouts i el controlador de pel·lícules).</p>
 */
@SpringBootTest(properties = "eureka.client.enabled=false")
class FrontendApplicationTests {

    @Test
    void contextLoads() {
    }
}
