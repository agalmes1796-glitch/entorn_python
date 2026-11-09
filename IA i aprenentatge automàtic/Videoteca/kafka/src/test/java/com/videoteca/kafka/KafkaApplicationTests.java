package com.videoteca.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Càrrega del context del microservici Kafka.
 *
 * <p>{@code eureka.client.enabled=false} desactiva el client de Eureka en el
 * test: de manera determinista i ràpida, sense reintents en background contra
 * {@code localhost:8761} (inexistent en l'entorn de test). El listener de Kafka
 * pot loguejar errors de conexió amb {@code localhost:9092} (també inexistent):
 * és acceptable; el que s'assegura amb aquest test és que el context s'obri.</p>
 */
@SpringBootTest(properties = "eureka.client.enabled=false")
class KafkaApplicationTests {

    @Test
    void contextLoads() {
    }
}
