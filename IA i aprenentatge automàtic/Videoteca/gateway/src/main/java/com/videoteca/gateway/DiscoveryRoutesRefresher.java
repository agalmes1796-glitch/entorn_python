package com.videoteca.gateway;

import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Refresc automàtic de les rutes del discovery-locator cada 30 s
 * (configurable amb {@code videoteca.gateway.route-refresh-delay-ms}).
 *
 * <p><b>Per què:</b> el gateway calcula les rutes del discovery-locator en
 * l'arrencada, abans que cap servei s'hagi registrat a Eureka, i no les
 * recalcula automàticament després. Sense aquest refresc, els serveis que es
 * registren després d'arrencar el gateway no rebrien rutes (404 a
 * {@code /gateway/**}) fins a un restart — carrera d'arrencada. Publicar
 * l'esdeveniment fa que el gateway recalculi les rutes amb les instàncies
 * registrades en l'instant actual.</p>
 *
 * <p><b>API (Spring Cloud 5.x):</b> es publica directament
 * {@link RefreshRoutesEvent}, l'esdeveniment de propòsit específic del
 * Spring Cloud Gateway: el {@code CachingRouteLocator} de 5.0.4 l'escucha
 * i recalcula les rutes a partir del {@code RouteDefinitionLocator} (inclosa
 * la del discovery-locator) amb les instàncies actuals. No depèn del
 * despatx intern d'altres escoltadors. No cal cap dependència addicional
 * (el jar de {@code spring-cloud-gateway-server-webflux} ja conté l'esdeveniment;
 * {@code spring-cloud-context} arriba com a dependència transitiva).</p>
 */
@Component
class DiscoveryRoutesRefresher {

    private final ApplicationEventPublisher publisher;

    DiscoveryRoutesRefresher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${videoteca.gateway.route-refresh-delay-ms:30000}")
    void refreshDiscoveryRoutes() {
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }
}
