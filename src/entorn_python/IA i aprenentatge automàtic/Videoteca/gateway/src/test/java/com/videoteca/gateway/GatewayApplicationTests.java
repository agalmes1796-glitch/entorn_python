package com.videoteca.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Càrrega del context del Gateway.
 *
 * <p>En l'entorn de test no hi ha cap servidor Eureka a {@code localhost:8761},
 * de manera que el registre/consulta del directori pot fallar i loguejar errors:
 * és acceptable; el que s'assegura amb aquest test és que el context s'obri.</p>
 */
@SpringBootTest
class GatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
