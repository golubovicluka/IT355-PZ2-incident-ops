package com.golubovicluka.incident_ops.authentication.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;

import com.golubovicluka.incident_ops.authentication.application.AuthenticatedSession;
import com.golubovicluka.incident_ops.authentication.application.Login;
import com.golubovicluka.incident_ops.shared.config.ApplicationClockConfiguration;
import com.golubovicluka.incident_ops.shared.security.ApiAccessDeniedHandler;
import com.golubovicluka.incident_ops.shared.security.ApiAuthenticationEntryPoint;
import com.golubovicluka.incident_ops.shared.security.JwtConfiguration;
import com.golubovicluka.incident_ops.shared.security.SecurityConfiguration;
import com.golubovicluka.incident_ops.shared.web.ApiErrorWriter;
import com.golubovicluka.incident_ops.shared.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoginController.class)
@Import({
		ApplicationClockConfiguration.class,
		AuthenticationExceptionHandler.class,
		ApiAccessDeniedHandler.class,
		ApiAuthenticationEntryPoint.class,
		ApiErrorWriter.class,
		ApiExceptionHandler.class,
		JwtConfiguration.class,
		SecurityConfiguration.class
})
@ActiveProfiles("test")
class LoginControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private Login login;

	@Test
	void validPublicLoginReturnsSessionContract() throws Exception {
		given(login.execute("responder", "correct-password"))
				.willReturn(new AuthenticatedSession(
						"signed.jwt.token",
						Instant.parse("2026-07-24T12:15:00Z"),
						"responder",
						"Response Engineer",
						Set.of("RESPONDER")));

		mockMvc.perform(post("/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "responder",
								  "password": "correct-password"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("signed.jwt.token"))
				.andExpect(jsonPath("$.expiresAt").value("2026-07-24T12:15:00Z"))
				.andExpect(jsonPath("$.username").value("responder"))
				.andExpect(jsonPath("$.displayName").value("Response Engineer"))
				.andExpect(jsonPath("$.roles[0]").value("RESPONDER"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void missingCredentialsReturnFieldErrorsInSharedApiErrorContract() throws Exception {
		mockMvc.perform(post("/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.timestamp").isString())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("Request validation failed"))
				.andExpect(jsonPath("$.path").value("/login"))
				.andExpect(jsonPath("$.fieldErrors.username").value("Username is required"))
				.andExpect(jsonPath("$.fieldErrors.password").value("Password is required"));
	}
}
