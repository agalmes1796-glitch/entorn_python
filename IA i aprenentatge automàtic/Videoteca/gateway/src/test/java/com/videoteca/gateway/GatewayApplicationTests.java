package com.videoteca.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Càrrega del context del Gateway.
 *
 * <p>{@code eureka.client.enabled=false} desactiva el client de Eureka en el
 * test: de manera determinista i ràpida, sense reintents en background contra
 * {@code localhost:8761} (inexistent en l'entorn de test). El que s'assegura
 * amb aquest test és que el context s'obri.</p>
 */
@SpringBootTest(properties = "eureka.client.enabled=false")
class GatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
