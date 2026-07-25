package com.golubovicluka.incident_ops.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.golubovicluka.incident_ops.identity.application.InitializeLocalIdentityData;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import com.golubovicluka.incident_ops.integration.PostgreSQLContainerSupport;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import com.golubovicluka.incident_ops.servicecatalog.domain.OwningTeam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthenticationSecurityIntegrationTest.ProtectedApiTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthenticationSecurityIntegrationTest extends PostgreSQLContainerSupport {

	private static final String TEST_PASSWORD = "responder-demo-password";
	private static final String TEST_SIGNING_SECRET =
			"test-only-incident-ops-signing-secret-never-use-outside-tests";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private InitializeLocalIdentityData initializeLocalIdentityData;

	@Autowired
	private UserAccountRepository users;

	@Autowired
	private ManagedServiceRepository services;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private JwtEncoder jwtEncoder;

	@BeforeEach
	void initializeUsers() {
		initializeLocalIdentityData.initialize();
	}

	@Test
	void validLoginReturnsSignedJwtWithExpectedIdentityAndRoleClaims() throws Exception {
		UserAccount responder = users.findByUsername("responder").orElseThrow();

		MvcResult result = mockMvc.perform(post("/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "responder",
								  "password": "responder-demo-password"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("responder"))
				.andExpect(jsonPath("$.displayName").value("Response Engineer"))
				.andExpect(jsonPath("$.roles[0]").value("RESPONDER"))
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		String token = JsonMapper.builder().build().readTree(responseBody).get("token").asString();
		Jwt jwt = jwtDecoder.decode(token);

		assertThat(jwt.getSubject()).isEqualTo("responder");
		assertThat(jwt.getClaimAsString("displayName")).isEqualTo("Response Engineer");
		assertThat(jwt.getClaimAsStringList("roles")).containsExactly("RESPONDER");
		assertThat(responseBody)
				.doesNotContain(TEST_PASSWORD)
				.doesNotContain(TEST_SIGNING_SECRET)
				.doesNotContain(responder.passwordHash())
				.doesNotContainIgnoringCase("password");
	}

	@Test
	void wrongPasswordReturnsGenericUnauthorizedErrorWithoutSensitiveData() throws Exception {
		UserAccount responder = users.findByUsername("responder").orElseThrow();

		MvcResult result = mockMvc.perform(post("/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "responder",
								  "password": "definitely-wrong"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Invalid username or password"))
				.andExpect(jsonPath("$.path").value("/login"))
				.andExpect(jsonPath("$.fieldErrors").doesNotExist())
				.andReturn();

		assertThat(result.getResponse().getContentAsString())
				.doesNotContain("definitely-wrong")
				.doesNotContain(TEST_SIGNING_SECRET)
				.doesNotContain(responder.passwordHash());
	}

	@Test
	void anonymousApiAccessReturnsSharedUnauthorizedError() throws Exception {
		mockMvc.perform(get("/api/test"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.path").value("/api/test"));
	}

	@Test
	void validJwtAuthenticatesApiRequest() throws Exception {
		String token = login("responder", TEST_PASSWORD);

		mockMvc.perform(get("/api/test")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subject").value("responder"));
	}

	@Test
	void incidentReporterAndCreatedEventActorComeFromJwtSubject()
			throws Exception {
		UserAccount responder = users.findByUsername("responder").orElseThrow();
		ManagedService managedService = services.save(ManagedService.create(
				"JWT Security Service " + System.nanoTime(),
				"Verifies incident ownership from the signed subject.",
				Criticality.HIGH,
				new OwningTeam(
						responder.team().id(),
						responder.team().name())));
		String token = login("responder", TEST_PASSWORD);

		mockMvc.perform(post("/api/incidents")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "JWT-owned incident",
								  "description": "Client ownership fields must be ignored.",
								  "priority": "SEV2",
								  "managedServiceId": %d,
								  "assigneeId": null,
								  "reporterId": 999999,
								  "eventActorId": 999998
								}
								""".formatted(managedService.id())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.reporter.id").value(responder.id()))
				.andExpect(jsonPath("$.reporter.username").value("responder"))
				.andExpect(jsonPath("$.timeline[0].kind").value("CREATED"))
				.andExpect(jsonPath("$.timeline[0].actor.id")
						.value(responder.id()))
				.andExpect(jsonPath("$.timeline[0].actor.username")
						.value("responder"));
	}

	@Test
	void invalidJwtReturnsSharedUnauthorizedError() throws Exception {
		mockMvc.perform(get("/api/test")
						.header(HttpHeaders.AUTHORIZATION, "Bearer definitely.not.a.jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message")
						.value("Authentication is required or the bearer token is invalid"))
				.andExpect(jsonPath("$.path").value("/api/test"));
	}

	@Test
	void expiredJwtReturnsSharedUnauthorizedError() throws Exception {
		String expiredToken = expiredToken();

		mockMvc.perform(get("/api/test")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.path").value("/api/test"));
	}

	@Test
	void responderJwtIsForbiddenFromAdministratorRoute() throws Exception {
		String responderToken = login("responder", TEST_PASSWORD);

		mockMvc.perform(get("/api/admin/test")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + responderToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.error").value("Forbidden"))
				.andExpect(jsonPath("$.message")
						.value("You do not have permission to access this resource"))
				.andExpect(jsonPath("$.path").value("/api/admin/test"));
	}

	@Test
	void administratorJwtCanAccessAdministratorRoute() throws Exception {
		String administratorToken = login("admin", "admin-demo-password");

		mockMvc.perform(get("/api/admin/test")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + administratorToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subject").value("admin"));
	}

	@Test
	void signedJwtWithoutSupportedRoleIsForbiddenFromApi() throws Exception {
		String unsupportedRoleToken = tokenWithRoles(List.of("AUDITOR"));

		mockMvc.perform(get("/api/test")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + unsupportedRoleToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.path").value("/api/test"));
	}

	@Test
	void responderJwtIsForbiddenFromDeletingIncident() throws Exception {
		String responderToken = login("responder", TEST_PASSWORD);

		mockMvc.perform(delete("/api/incidents/42")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + responderToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.path").value("/api/incidents/42"));
	}

	@Test
	void administratorJwtCanDeleteIncident() throws Exception {
		UserAccount administrator = users.findByUsername("admin").orElseThrow();
		ManagedService managedService = services.save(ManagedService.create(
				"Administrator Delete Service " + System.nanoTime(),
				"Verifies incident deletion through the signed administrator.",
				Criticality.HIGH,
				new OwningTeam(
						administrator.team().id(),
						administrator.team().name())));
		String administratorToken = login("admin", "admin-demo-password");
		MvcResult created = mockMvc.perform(post("/api/incidents")
						.header(
								HttpHeaders.AUTHORIZATION,
								"Bearer " + administratorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Incident to delete",
								  "description": "Created to verify administrator deletion.",
								  "priority": "SEV3",
								  "managedServiceId": %d,
								  "assigneeId": null
								}
								""".formatted(managedService.id())))
				.andExpect(status().isCreated())
				.andReturn();
		long incidentId = JsonMapper.builder()
				.build()
				.readTree(created.getResponse().getContentAsString())
				.get("id")
				.asLong();

		mockMvc.perform(delete("/api/incidents/{id}", incidentId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + administratorToken))
				.andExpect(status().isNoContent());
	}

	@Test
	void configuredCorsOriginCanMakeApiPreflightRequest() throws Exception {
		mockMvc.perform(options("/api/test")
						.header(HttpHeaders.ORIGIN, "http://localhost:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
						"http://localhost:5173"))
				.andExpect(header().string(
						HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
						"true"));
	}

	@Test
	void unconfiguredCorsOriginIsRejected() throws Exception {
		mockMvc.perform(options("/api/test")
						.header(HttpHeaders.ORIGIN, "https://untrusted.example")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	private String login(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "password": "%s"
								}
								""".formatted(username, password)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonMapper.builder()
				.build()
				.readTree(result.getResponse().getContentAsString())
				.get("token")
				.asString();
	}

	private String expiredToken() {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject("responder")
				.issuedAt(now.minusSeconds(300))
				.expiresAt(now.minusSeconds(120))
				.claim("displayName", "Response Engineer")
				.claim("roles", List.of("RESPONDER"))
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}

	private String tokenWithRoles(List<String> roles) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject("external-user")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(300))
				.claim("displayName", "External User")
				.claim("roles", roles)
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ProtectedApiTestConfiguration {

		@Bean
		ProtectedApiTestController protectedApiTestController() {
			return new ProtectedApiTestController();
		}
	}

	@RestController
	static class ProtectedApiTestController {

		@GetMapping({"/api/test", "/api/admin/test"})
		Map<String, String> currentSubject(Authentication authentication) {
			return Map.of("subject", authentication.getName());
		}
	}
}
