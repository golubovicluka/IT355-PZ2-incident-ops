package com.golubovicluka.incident_ops.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiErrorContractIntegrationTest.FailingController.class)
class ApiErrorContractIntegrationTest extends PostgreSQLContainerSupport {

	private static final String EMPTY_JSON = "{}";
	private static final String INVALID_INCIDENT = """
			{
			  "title": "",
			  "description": "",
			  "priority": null,
			  "managedServiceId": null
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@ParameterizedTest(name = "{0}")
	@MethodSource("endpointErrorCases")
	void endpointFamiliesUseTheSharedErrorContract(
			String description,
			MockHttpServletRequestBuilder request,
			int expectedStatus,
			String expectedPath,
			String expectedField) throws Exception {
		var result = mockMvc.perform(request)
				.andExpect(status().is(expectedStatus))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.timestamp").isString())
				.andExpect(jsonPath("$.status").value(expectedStatus))
				.andExpect(jsonPath("$.error").isString())
				.andExpect(jsonPath("$.message").isString())
				.andExpect(jsonPath("$.path").value(expectedPath));

		if (expectedField == null) {
			result.andExpect(jsonPath("$.fieldErrors").doesNotExist());
		}
		else {
			result.andExpect(jsonPath("$.fieldErrors." + expectedField).isString());
		}
	}

	@Test
	void unexpectedFailuresReturnSanitizedSharedErrorContract() throws Exception {
		mockMvc.perform(get("/api/test/failure").with(responder()))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.timestamp").isString())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.error").value("Internal Server Error"))
				.andExpect(jsonPath("$.message").value("An unexpected error occurred"))
				.andExpect(jsonPath("$.message", not(containsString("sensitive"))))
				.andExpect(jsonPath("$.path").value("/api/test/failure"))
				.andExpect(jsonPath("$.fieldErrors").doesNotExist());
	}

	private static Stream<Arguments> endpointErrorCases() {
		return Stream.of(
				errorCase(
						"login validation",
						post("/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(EMPTY_JSON),
						400,
						"/login",
						"username"),
				errorCase(
						"login content type",
						post("/login")
								.contentType(MediaType.TEXT_PLAIN)
								.content("not-json"),
						415,
						"/login",
						null),
				errorCase(
						"login method",
						get("/login"),
						405,
						"/login",
						null),
				errorCase(
						"team create validation",
						post("/api/admin/teams")
								.with(admin())
								.contentType(MediaType.APPLICATION_JSON)
								.content(EMPTY_JSON),
						400,
						"/api/admin/teams",
						"name"),
				errorCase(
						"team path parameter",
						delete("/api/admin/teams/not-a-number").with(admin()),
						400,
						"/api/admin/teams/not-a-number",
						"id"),
				errorCase(
						"service create validation",
						post("/api/admin/services")
								.with(admin())
								.contentType(MediaType.APPLICATION_JSON)
								.content(EMPTY_JSON),
						400,
						"/api/admin/services",
						"name"),
				errorCase(
						"policy create validation",
						post("/api/admin/policies")
								.with(admin())
								.contentType(MediaType.APPLICATION_JSON)
								.content(EMPTY_JSON),
						400,
						"/api/admin/policies",
						"managedServiceId"),
				errorCase(
						"incident create validation",
						post("/api/incidents")
								.with(responder())
								.contentType(MediaType.APPLICATION_JSON)
								.content(INVALID_INCIDENT),
						400,
						"/api/incidents",
						"title"),
				errorCase(
						"incident update validation",
						put("/api/incidents/1")
								.with(responder())
								.contentType(MediaType.APPLICATION_JSON)
								.content(INVALID_INCIDENT),
						400,
						"/api/incidents/1",
						"title"),
				errorCase(
						"incident status validation",
						put("/api/incidents/1/status")
								.with(responder())
								.contentType(MediaType.APPLICATION_JSON)
								.content(EMPTY_JSON),
						400,
						"/api/incidents/1/status",
						"status"),
				errorCase(
						"incident note validation",
						post("/api/incidents/1/events")
								.with(responder())
								.contentType(MediaType.APPLICATION_JSON)
								.content(EMPTY_JSON),
						400,
						"/api/incidents/1/events",
						"note"),
				errorCase(
						"incident filter validation",
						get("/api/incidents")
								.with(responder())
								.queryParam("priority", "UNKNOWN"),
						400,
						"/api/incidents",
						"priority"),
				errorCase(
						"incident escalation validation",
						post("/api/incidents/1/escalations")
								.with(responder())
								.contentType(MediaType.APPLICATION_JSON)
								.content(EMPTY_JSON),
						400,
						"/api/incidents/1/escalations",
						"reason"),
				errorCase(
						"anonymous user catalog",
						get("/api/catalogs/users"),
						401,
						"/api/catalogs/users",
						null),
				errorCase(
						"anonymous service catalog",
						get("/api/catalogs/services"),
						401,
						"/api/catalogs/services",
						null),
				errorCase(
						"anonymous analytics",
						get("/api/analytics/summary"),
						401,
						"/api/analytics/summary",
						null),
				errorCase(
						"responder administration denial",
						get("/api/admin/teams").with(responder()),
						403,
						"/api/admin/teams",
						null),
				errorCase(
						"responder incident deletion denial",
						delete("/api/incidents/1").with(responder()),
						403,
						"/api/incidents/1",
						null),
				errorCase(
						"unknown API route",
						get("/api/does-not-exist").with(responder()),
						404,
						"/api/does-not-exist",
						null));
	}

	private static Arguments errorCase(
			String description,
			MockHttpServletRequestBuilder request,
			int status,
			String path,
			String field) {
		return Arguments.of(description, request, status, path, field);
	}

	private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor responder() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_RESPONDER"));
	}

	private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor admin() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

	@RestController
	static class FailingController {

		@GetMapping("/api/test/failure")
		void fail() {
			throw new IllegalStateException("sensitive internal class and SQL details");
		}
	}
}
