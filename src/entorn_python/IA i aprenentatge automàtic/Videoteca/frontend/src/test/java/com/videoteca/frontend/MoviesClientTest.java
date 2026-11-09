package com.videoteca.frontend;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prova del client HTTP amb un servidor HTTP local efímer (cap petició real
 * al gateway).
 *
 * <p>Verifica (1) que el client crida exactament la ruta
 * {@code {gateway.base-url}/movies} i mapeja el JSON de snake_case del
 * gateway al record {@link Pelicula}, i (2) que, si el gateway no és
 * reachable (connexió rebutjada), es llança
 * {@link MoviesUnavailableException} — que el controlador converteix en
 * HTTP 503.</p>
 */
class MoviesClientTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private HttpServer server;

    @AfterEach
    void apagaElServidor() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /**
     * Crea un servidor efímer a 127.0.0.1 que només respon 200 amb un JSON de
     * prova a la ruta {@code /movies} (qualsevol altra ruta rep 404, la qual
     * cosa fallaria la prova si el client cridés una ruta diferent).
     */
    private HttpServer arrancaServidorAmbPeliculesDeProva() throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", exchange -> {
            if (!"/movies".equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            byte[] cos = """
                    [{"id":1,"titulo":"El viatger","sinopsis":"Una sinopsi qualsevol",\
                    "anio_estreno":2020,"duracion_minutos":120,"genero_id":7,"director_id":3}]
                    """.getBytes(StandardCharsets.UTF_8);
            // El gateway real (Spring MVC) respondria amb aquests capçaleres.
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, cos.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(cos);
            }
        });
        servidor.start();
        return servidor;
    }

    private static RestClient restClient(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(TIMEOUT);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Test
    void mapejaElJsonDeSnakeCaseAlDto() throws Exception {
        server = arrancaServidorAmbPeliculesDeProva();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        MoviesClient client = new MoviesClient(restClient(baseUrl));

        List<Pelicula> pelicules = client.fetchMovies();

        assertThat(pelicules).hasSize(1);
        Pelicula pelicula = pelicules.getFirst();
        assertThat(pelicula.id()).isEqualTo(1);
        assertThat(pelicula.titulo()).isEqualTo("El viatger");
        assertThat(pelicula.sinopsis()).isEqualTo("Una sinopsi qualsevol");
        assertThat(pelicula.anio_estreno()).isEqualTo(2020);
        assertThat(pelicula.duracion_minutos()).isEqualTo(120);
        assertThat(pelicula.genero_id()).isEqualTo(7);
        assertThat(pelicula.director_id()).isEqualTo(3);
    }

    @Test
    void lancaMoviesUnavailableQuanElGatewayNoEsReachable() throws Exception {
        // Port lliure amb cap escoltador: la connexió serà rebutjada.
        int portTancat;
        try (ServerSocket sondeig = new ServerSocket(0)) {
            portTancat = sondeig.getLocalPort();
        }

        MoviesClient client = new MoviesClient(restClient("http://127.0.0.1:" + portTancat));

        assertThatThrownBy(client::fetchMovies)
                .isInstanceOf(MoviesUnavailableException.class)
                .hasMessageContaining("gateway");
    }
}
