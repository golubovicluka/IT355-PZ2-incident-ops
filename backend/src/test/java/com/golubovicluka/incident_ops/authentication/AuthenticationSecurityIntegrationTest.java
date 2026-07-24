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
import org.springframework.web.bind.annotation.DeleteMapping;
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
		String administratorToken = login("admin", "admin-demo-password");

		mockMvc.perform(delete("/api/incidents/42")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + administratorToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subject").value("admin"));
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

		@DeleteMapping("/api/incidents/{id}")
		Map<String, String> deleteIncident(Authentication authentication) {
			return Map.of("subject", authentication.getName());
		}
	}
}
