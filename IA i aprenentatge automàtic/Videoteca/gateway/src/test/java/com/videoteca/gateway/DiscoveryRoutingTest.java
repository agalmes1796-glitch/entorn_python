package com.videoteca.gateway;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.route.CachingRouteLocator;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova d'enrutament del discovery-locator, sense Eureka real.
 *
 * <p>Defineix un {@link ReactiveDiscoveryClient} de prova que "registra" un
 * servei fictici ({@code stubservice}) — agrupat pel
 * {@code reactiveCompositeDiscoveryClient} {@code @Primary} de
 * spring-cloud-commons, el client que rep el discovery-locator — i verifica
 * que el gateway generi la ruta {@code /stubservice/**} (URI
 * {@code lb://stubservice}, id amb {@code stubservice} i predicat Path).
 * La prova falla si algú torna a
 * escriure el namespace legat {@code spring.cloud.gateway.discovery.locator.*}
 * al {@code application.yml} (el bean {@code DiscoveryClientRouteDefinitionLocator}
 * de 5.x exigeix {@code spring.cloud.gateway.server.webflux.discovery.locator.enabled}
 * i no es crearia amb el namespace antic), i passa amb el namespace de 5.x.
 * {@code eureka.client.enabled=false} manté la prova determinista i ràpida.</p>
 */
@SpringBootTest(properties = "eureka.client.enabled=false")
class DiscoveryRoutingTest {

    @Autowired
    private CachingRouteLocator routeLocator;

    @Test
    void discoveryLocatorGeneraRutaPerAlServeiRegistrat() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        // La ruta del discovery-locator apunta a lb://stubservice. En 5.x el id
        // porta el prefix del client (p. ex.
        // 'ReactiveCompositeDiscoveryClient_stubservice'), per tant s'ancora a
        // l'URI i es comprova l'id amb contains.
        Route route = routes.stream()
                .filter(r -> r.getUri() != null && "stubservice".equals(r.getUri().getHost()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Falta la ruta de discovery per a 'stubservice'. Rutes actuals: "
                                + routes.stream().map(Route::getId).toList()));

        // URI lb://stubservice (balancejat per instàncies del servei al directori).
        assertThat(route.getUri().getScheme()).isEqualTo("lb");
        assertThat(route.getId()).contains("stubservice");

        // El predicat Path coincideix amb /stubservice/** (predicat asíncron en 5.x).
        MockServerWebExchange matching = MockServerWebExchange.from(
                MockServerHttpRequest.get("/stubservice/qualsevol").build());
        MockServerWebExchange notMatching = MockServerWebExchange.from(
                MockServerHttpRequest.get("/altreservei/qualsevol").build());
        assertThat(Flux.from(route.getPredicate().apply(matching)).blockFirst()).isTrue();
        assertThat(Flux.from(route.getPredicate().apply(notMatching)).blockFirst()).isFalse();
    }

    /**
     * Directori de prova amb un únic servei fictici registrat.
     *
     * <p>Cal usar {@code @TestConfiguration} (no pas {@code @Configuration}):
     * amb aquest darrer, la configuració d'usuari impedeix que s'apliqui la
     * autoconfiguració del gateway en aquest entorn de test i manquen els
     * beans de rutes. El bean NO porta {@code @Primary}: el
     * {@code reactiveCompositeDiscoveryClient} de spring-cloud-commons 5.x
     * ja és {@code @Primary} i agraupa tots els clients registrats (inclos
     * aquest stub), i un segon {@code @Primary} provocaria
     * {@code NoUniqueBeanDefinitionException} al bean del discovery-locator.</p>
     */
    @TestConfiguration
    static class StubDiscoveryConfig {

        @Bean
        ReactiveDiscoveryClient stubDiscoveryClient() {
            return new ReactiveDiscoveryClient() {
                @Override
                public String description() {
                    return "stub-discovery-client (test)";
                }

                @Override
                public Flux<String> getServices() {
                    return Flux.just("stubservice");
                }

                @Override
                public Flux<ServiceInstance> getInstances(String serviceId) {
                    return Flux.just(
                            new DefaultServiceInstance("stubservice-1", serviceId, "localhost", 9999, false));
                }
            };
        }
    }
}
