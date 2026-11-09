package com.videoteca.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Càrrega del context del Frontend.
 *
 * <p>En l'entorn de test no hi ha cap servidor Eureka a {@code localhost:8761}
 * ni cap gateway, de manera que el registre al directori pot fallar i loguejar
 * errors: és acceptable; el que s'assegura amb aquest test és que el context
 * s'obri (i amb ell el bean de {@code RestClient} per al gateway).</p>
 */
@SpringBootTest
class FrontendApplicationTests {

    @Test
    void contextLoads() {
    }
}
