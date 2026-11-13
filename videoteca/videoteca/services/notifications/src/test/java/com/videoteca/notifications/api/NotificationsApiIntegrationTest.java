package com.videoteca.notifications.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración: arrancan el contexto completo (web MOCK) y ejercitan
 * TODOS los endpoints con MockMvc.
 *
 * <p>El {@link JavaMailSender} se sustituye por un mock mediante
 * {@code @MockitoBean}; nunca se toca un SMTP real.</p>
 */
@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class NotificationsApiIntegrationTest {

	private static final String OUTBOUND_BASE = "/api/v1/notifications/outbound";

	private static final String INBOUND_BASE = "/api/v1/notifications/inbound";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JavaMailSender javaMailSender;

	@BeforeEach
	void resetMailSender() {
		Mockito.reset(javaMailSender);
	}

	// ---------------------------------------------------------------------
	// Actuator
	// ---------------------------------------------------------------------

	@Test
	void actuatorHealthIsUpWithoutSmtpServer() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));
	}

	// ---------------------------------------------------------------------
	// Outbound: POST /send
	// ---------------------------------------------------------------------

	@Test
	void sendOutboundReturns202AndRegistersSentRecord() throws Exception {
		String json = """
				{
					"to": "usuario@dominio.com",
					"cc": ["copias@dominio.com"],
					"subject": "Película añadida",
					"body": "La película Blade Runner fue añadida al catálogo.",
					"type": "MOVIE_ADDED",
					"referenceId": "123",
					"payload": { "movieId": 42, "title": "Blade Runner", "year": 1982 }
				}
				""";

		MvcResult result = mockMvc.perform(post(OUTBOUND_BASE + "/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.status").value("SENT"))
			.andExpect(jsonPath("$.to").value("usuario@dominio.com"))
			.andExpect(jsonPath("$.type").value("MOVIE_ADDED"))
			.andExpect(jsonPath("$.sentAt").isNotEmpty())
			.andReturn();

		String id = readId(result);

		// El registro es visible en el detalle
		mockMvc.perform(get(OUTBOUND_BASE + "/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.status").value("SENT"))
			.andExpect(jsonPath("$.to").value("usuario@dominio.com"))
			.andExpect(jsonPath("$.cc[0]").value("copias@dominio.com"))
			.andExpect(jsonPath("$.subject").value("Película añadida"))
			.andExpect(jsonPath("$.referenceId").value("123"))
			.andExpect(jsonPath("$.payload.movieId").value(42))
			.andExpect(jsonPath("$.createdAt").isNotEmpty());

		verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
	}

	@Test
	void sendOutboundWithSmtpFailureReturns202AndRegistersFailedRecord() throws Exception {
		doThrow(new MailSendException("Connection refused: localhost/127.0.0.1:25"))
			.when(javaMailSender).send(any(SimpleMailMessage.class));

		String json = """
				{
					"to": "usuario@dominio.com",
					"subject": "Película añadida",
					"body": "La película Blade Runner fue añadida al catálogo.",
					"type": "MOVIE_ADDED",
					"referenceId": "123",
					"payload": { "movieId": 42 }
				}
				""";

		MvcResult result = mockMvc.perform(post(OUTBOUND_BASE + "/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.status").value("FAILED"))
			// Mensaje sanitizado: nunca el detalle crudo de JavaMail
			.andExpect(jsonPath("$.errorMessage").value("Fallo de envío SMTP"))
			.andExpect(jsonPath("$.sentAt").value((Object) null))
			.andReturn();

		String id = readId(result);

		// El registro existe igualmente (no se lanzó 500) y el error sigue sanitizado
		mockMvc.perform(get(OUTBOUND_BASE + "/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("FAILED"))
			.andExpect(jsonPath("$.errorMessage").value("Fallo de envío SMTP"));
	}

	@Test
	void sendOutboundReturns400WhenToIsNotAValidEmail() throws Exception {
		String json = """
				{ "to": "no-es-un-email", "subject": "s", "body": "b", "type": "MOVIE_ADDED" }
				""";

		mockMvc.perform(post(OUTBOUND_BASE + "/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Bad Request"))
			.andExpect(jsonPath("$.errors[0].field").value("to"))
			.andExpect(jsonPath("$.errors[0].message").isNotEmpty());
	}

	@Test
	void sendOutboundReturns400WhenSubjectIsBlank() throws Exception {
		String json = """
				{ "to": "usuario@dominio.com", "subject": "", "body": "b", "type": "MOVIE_ADDED" }
				""";

		mockMvc.perform(post(OUTBOUND_BASE + "/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Bad Request"))
			.andExpect(jsonPath("$.errors[0].field").value("subject"));
	}

	@Test
	void sendOutboundReturns400WhenMandatoryFieldsAreMissing() throws Exception {
		mockMvc.perform(post(OUTBOUND_BASE + "/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			// 4 campos obligatorios: to, subject, body, type
			.andExpect(jsonPath("$.error").value("Bad Request"))
			.andExpect(jsonPath("$.errors", hasSize(4)));
	}

	@Test
	void sendOutboundReturns400WhenSubjectExceedsMaxLength() throws Exception {
		String longSubject = "x".repeat(201);
		String json = """
				{ "to": "usuario@dominio.com", "subject": "%s", "body": "b", "type": "MOVIE_ADDED" }
				""".formatted(longSubject);

		mockMvc.perform(post(OUTBOUND_BASE + "/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("subject"));
	}

	@Test
	void sendOutboundReturns400ForMalformedJson() throws Exception {
		mockMvc.perform(post(OUTBOUND_BASE + "/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ json malformado"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Bad Request"));
	}

	@Test
	void sendOutboundReturns400WhenCcIsNotAValidEmail() throws Exception {
		String json = """
				{ "to": "usuario@dominio.com", "cc": ["no-es-email"], "subject": "s", "body": "b", "type": "MOVIE_ADDED" }
				""";

		mockMvc.perform(post(OUTBOUND_BASE + "/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			// El error de elemento se reporta como "cc[0]"
			.andExpect(jsonPath("$.errors[*].field", org.hamcrest.Matchers.everyItem(
					org.hamcrest.Matchers.startsWith("cc"))));
	}

	@Test
	void sendOutboundReturns400WhenCcExceedsMaxSize() throws Exception {
		// 21 cc > tope de 20
		StringBuilder cc = new StringBuilder("[");
		for (int i = 0; i < 21; i++) {
			if (i > 0) {
				cc.append(",");
			}
			cc.append("\"cc").append(i).append("@dominio.com\"");
		}
		cc.append("]");
		String json = """
				{ "to": "usuario@dominio.com", "cc": %s, "subject": "s", "body": "b", "type": "MOVIE_ADDED" }
				""".formatted(cc);

		mockMvc.perform(post(OUTBOUND_BASE + "/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			// Tope de lista: el error se reporta en el campo "cc"
			.andExpect(jsonPath("$.errors[*].field", org.hamcrest.Matchers.everyItem(
					org.hamcrest.Matchers.startsWith("cc"))));
	}

	@Test
	void sendOutboundReturns400WhenBodyExceedsMaxLength() throws Exception {
		String longBody = "x".repeat(20_001);
		String json = """
				{ "to": "usuario@dominio.com", "subject": "s", "body": "%s", "type": "MOVIE_ADDED" }
				""".formatted(longBody);

		mockMvc.perform(post(OUTBOUND_BASE + "/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("body"));
	}

	@Test
	void listOutboundReturns400WhenLimitExceedsMax() throws Exception {
		mockMvc.perform(get(OUTBOUND_BASE).param("limit", "1001"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Bad Request"));
	}

	@Test
	void unknownRouteReturns404Json() throws Exception {
		mockMvc.perform(get("/api/v1/notifications/ruta-inexistente"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("Not Found"))
			.andExpect(jsonPath("$.message").isNotEmpty())
			.andExpect(jsonPath("$.path").value("/api/v1/notifications/ruta-inexistente"));
	}

	// ---------------------------------------------------------------------
	// Outbound: GET list / detail / resend
	// ---------------------------------------------------------------------

	@Test
	void listOutboundFiltersByStatusAndAppliesLimit() throws Exception {
		// Tipos únicos por test para no interferir con el store compartido del contexto
		String failedId = createOutbound("LISTA_" + System.nanoTime(), true);
		String sentId = createOutbound("LISTA_" + System.nanoTime(), false);

		// Filtro status: todos los elementos devueltos cumplen el filtro y
		// nuestro registro creado aparece
		mockMvc.perform(get(OUTBOUND_BASE).param("status", "SENT"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].status", everyItem(is("SENT"))));
		mockMvc.perform(get(OUTBOUND_BASE).param("status", "FAILED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].status", everyItem(is("FAILED"))));

		// El detalle sigue funcionando con los ids creados
		mockMvc.perform(get(OUTBOUND_BASE + "/" + sentId)).andExpect(jsonPath("$.id").value(sentId));
		mockMvc.perform(get(OUTBOUND_BASE + "/" + failedId)).andExpect(jsonPath("$.id").value(failedId));

		// Limit
		mockMvc.perform(get(OUTBOUND_BASE).param("limit", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)));

		// Limit inválido → 400
		mockMvc.perform(get(OUTBOUND_BASE).param("limit", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Bad Request"));

		// Status inválido → 400
		mockMvc.perform(get(OUTBOUND_BASE).param("status", "PENDING"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Bad Request"));
	}

	@Test
	void getOutboundReturns404ForUnknownId() throws Exception {
		String unknownId = UUID.randomUUID().toString();

		mockMvc.perform(get(OUTBOUND_BASE + "/" + unknownId))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("Not Found"))
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(unknownId)))
			.andExpect(jsonPath("$.path").value(OUTBOUND_BASE + "/" + unknownId));
	}

	@Test
	void getOutboundReturns404ForMalformedId() throws Exception {
		mockMvc.perform(get(OUTBOUND_BASE + "/no-un-uuid"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Bad Request"));
	}

	@Test
	void resendOutboundRetriesDeliveryAndReturns200WithNewStatus() throws Exception {
		// 1er envío: SMTP cae → FAILED
		String failedId = createOutbound("RESEND_" + System.nanoTime(), true);

		mockMvc.perform(get(OUTBOUND_BASE + "/" + failedId))
			.andExpect(jsonPath("$.status").value("FAILED"));

		// El mock "recupera": resetea la estiba
		Mockito.reset(javaMailSender);

		mockMvc.perform(post(OUTBOUND_BASE + "/" + failedId + "/resend"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(failedId))
			.andExpect(jsonPath("$.status").value("SENT"))
			.andExpect(jsonPath("$.sentAt").isNotEmpty())
			.andExpect(jsonPath("$.errorMessage").value((Object) null));

		// El estado persistido también actualiza
		mockMvc.perform(get(OUTBOUND_BASE + "/" + failedId))
			.andExpect(jsonPath("$.status").value("SENT"));
	}

	@Test
	void resendOutboundReturns404ForUnknownId() throws Exception {
		mockMvc.perform(post(OUTBOUND_BASE + "/" + UUID.randomUUID() + "/resend"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("Not Found"));
	}

	@Test
	void resendOutboundReturns409WhenNotificationAlreadySent() throws Exception {
		// El envío sale bien → registro SENT
		String sentId = createOutbound("RESEND409_" + System.nanoTime(), false);

		mockMvc.perform(get(OUTBOUND_BASE + "/" + sentId))
			.andExpect(jsonPath("$.status").value("SENT"));

		// Reenviar una SENT → 409 Conflict, sin reenviar nada
		mockMvc.perform(post(OUTBOUND_BASE + "/" + sentId + "/resend"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("Conflict"))
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(sentId)))
			.andExpect(jsonPath("$.path").value(OUTBOUND_BASE + "/" + sentId + "/resend"));
	}

	// ---------------------------------------------------------------------
	// Inbound: POST /
	// ---------------------------------------------------------------------

	@Test
	void createInboundReturns201WithUnreadRecord() throws Exception {
		String json = """
				{
					"sender": "movies",
					"type": "MOVIE_REMOVED",
					"subject": "Película retirada",
					"body": "La película 42 fue retirada del catálogo.",
					"referenceId": "42",
					"payload": { "movieId": 42, "title": "Blade Runner" }
				}
				""";

		MvcResult result = mockMvc.perform(post(INBOUND_BASE)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.sender").value("movies"))
			.andExpect(jsonPath("$.type").value("MOVIE_REMOVED"))
			.andExpect(jsonPath("$.subject").value("Película retirada"))
			.andExpect(jsonPath("$.read").value(false))
			.andExpect(jsonPath("$.receivedAt").isNotEmpty())
			.andExpect(jsonPath("$.referenceId").value("42"))
			.andExpect(jsonPath("$.payload.movieId").value(42))
			.andReturn();

		// Visible en el detalle
		String id = readId(result);
		mockMvc.perform(get(INBOUND_BASE + "/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.read").value(false));
	}

	@Test
	void createInboundReturns400WhenSenderIsMissing() throws Exception {
		String json = """
				{ "type": "MOVIE_REMOVED", "subject": "s", "body": "b" }
				""";

		mockMvc.perform(post(INBOUND_BASE)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Bad Request"))
			.andExpect(jsonPath("$.errors[0].field").value("sender"));
	}

	@Test
	void createInboundReturns400WhenTypeIsBlank() throws Exception {
		String json = """
				{ "sender": "movies", "type": " ", "subject": "s", "body": "b" }
				""";

		mockMvc.perform(post(INBOUND_BASE)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("type"));
	}

	@Test
	void createInboundReturns400WhenSubjectExceedsMaxLength() throws Exception {
		String longSubject = "x".repeat(201);
		String json = """
				{ "sender": "movies", "type": "MOVIE_REMOVED", "subject": "%s", "body": "b" }
				""".formatted(longSubject);

		mockMvc.perform(post(INBOUND_BASE)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("subject"));
	}

	// ---------------------------------------------------------------------
	// Inbound: GET list / detail
	// ---------------------------------------------------------------------

	@Test
	void listInboundFiltersByReadTypeAndLimit() throws Exception {
		String type = "FILTERS_" + System.nanoTime();
		String readId = createInbound("movies", type, true);
		String unreadId = createInbound("movies", type, false);

		mockMvc.perform(get(INBOUND_BASE).param("type", type).param("read", "true"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(readId))
			.andExpect(jsonPath("$[0].read").value(true));

		mockMvc.perform(get(INBOUND_BASE).param("type", type).param("read", "false"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(unreadId))
			.andExpect(jsonPath("$[0].read").value(false));

		mockMvc.perform(get(INBOUND_BASE).param("type", type))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(get(INBOUND_BASE).param("type", type).param("limit", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(get(INBOUND_BASE).param("limit", "0"))
			.andExpect(status().isBadRequest());

		// limit > 1000 → 400
		mockMvc.perform(get(INBOUND_BASE).param("limit", "1001"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Bad Request"));
	}

	@Test
	void getInboundReturns404ForUnknownId() throws Exception {
		String unknownId = UUID.randomUUID().toString();

		mockMvc.perform(get(INBOUND_BASE + "/" + unknownId))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("Not Found"))
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(unknownId)))
			.andExpect(jsonPath("$.path").value(INBOUND_BASE + "/" + unknownId));
	}

	// ---------------------------------------------------------------------
	// Inbound: PATCH /{id}/read
	// ---------------------------------------------------------------------

	@Test
	void patchInboundReadWithBody() throws Exception {
		String id = createInbound("movies", "PATCH_" + System.nanoTime(), false);

		mockMvc.perform(patch(INBOUND_BASE + "/" + id + "/read")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"read\": true }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.read").value(true));

		mockMvc.perform(patch(INBOUND_BASE + "/" + id + "/read")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"read\": false }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.read").value(false));

		// Persistido
		mockMvc.perform(get(INBOUND_BASE + "/" + id))
			.andExpect(jsonPath("$.read").value(false));
	}

	@Test
	void patchInboundReadWithQueryParam() throws Exception {
		String id = createInbound("movies", "PATCH_" + System.nanoTime(), false);

		mockMvc.perform(patch(INBOUND_BASE + "/" + id + "/read").param("read", "true"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.read").value(true));
	}

	@Test
	void patchInboundReadWithoutValueReturns400() throws Exception {
		String id = createInbound("movies", "PATCH_" + System.nanoTime(), false);

		mockMvc.perform(patch(INBOUND_BASE + "/" + id + "/read"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Bad Request"))
			.andExpect(jsonPath("$.errors[0].field").value("request"));
	}

	@Test
	void patchInboundReadReturns404ForUnknownId() throws Exception {
		mockMvc.perform(patch(INBOUND_BASE + "/" + UUID.randomUUID() + "/read").param("read", "true"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("Not Found"));
	}

	// ---------------------------------------------------------------------
	// Inbound: DELETE /{id}
	// ---------------------------------------------------------------------

	@Test
	void deleteInboundReturns204AndSubsequentGetReturns404() throws Exception {
		String id = createInbound("movies", "DELETE_" + System.nanoTime(), false);

		mockMvc.perform(delete(INBOUND_BASE + "/" + id))
			.andExpect(status().isNoContent());

		mockMvc.perform(get(INBOUND_BASE + "/" + id))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("Not Found"));
	}

	@Test
	void deleteInboundReturns404ForUnknownId() throws Exception {
		mockMvc.perform(delete(INBOUND_BASE + "/" + UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("Not Found"));
	}

	// ---------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------

	private String readId(MvcResult result) throws Exception {
		JsonMapper mapper = new JsonMapper();
		JsonNode root = mapper.readTree(result.getResponse().getContentAsString());
		return root.get("id").asText();
	}

	/** Crea una outbound vía API; si failSmtp=true el envío queda FAILED. Devuelve su id. */
	private String createOutbound(String type, boolean failSmtp) throws Exception {
		if (failSmtp) {
			doThrow(new MailSendException("smtp unavailable")).when(javaMailSender).send(any(SimpleMailMessage.class));
		}
		String json = """
				{
					"to": "usuario%d@dominio.com",
					"subject": "s",
					"body": "b",
					"type": "%s"
				}
				""".formatted(System.nanoTime(), type);
		MvcResult result = mockMvc.perform(post(OUTBOUND_BASE + "/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isAccepted())
			.andReturn();
		return readId(result);
	}

	/** Crea una inbound vía API y opcionalmente la marca como leída. Devuelve su id. */
	private String createInbound(String sender, String type, boolean markAsRead) throws Exception {
		String json = """
				{
					"sender": "%s",
					"type": "%s",
					"subject": "s",
					"body": "b"
				}
				""".formatted(sender, type);
		MvcResult result = mockMvc.perform(post(INBOUND_BASE)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isCreated())
			.andReturn();
		String id = readId(result);
		if (markAsRead) {
			mockMvc.perform(patch(INBOUND_BASE + "/" + id + "/read").param("read", "true"))
				.andExpect(status().isOk());
		}
		return id;
	}

}
