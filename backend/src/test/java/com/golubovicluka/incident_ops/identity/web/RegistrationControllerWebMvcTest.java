package com.golubovicluka.incident_ops.identity.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import com.golubovicluka.incident_ops.identity.application.RegisterUserAccount;
import com.golubovicluka.incident_ops.identity.application.RegistrationUnavailableException;
import com.golubovicluka.incident_ops.identity.application.UserAccountView;
import com.golubovicluka.incident_ops.identity.application.command.RegisterUserAccountCommand;
import com.golubovicluka.incident_ops.identity.domain.DuplicateUsernameException;
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

@WebMvcTest(RegistrationController.class)
@Import({
		ApplicationClockConfiguration.class,
		RegistrationExceptionHandler.class,
		ApiAccessDeniedHandler.class,
		ApiAuthenticationEntryPoint.class,
		ApiErrorWriter.class,
		ApiExceptionHandler.class,
		JwtConfiguration.class,
		SecurityConfiguration.class
})
@ActiveProfiles("test")
class RegistrationControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RegisterUserAccount registerUserAccount;

	@Test
	void validPublicRegistrationCreatesLeastPrivilegeAccount() throws Exception {
		given(registerUserAccount.execute(new RegisterUserAccountCommand(
				"new.responder",
				"New Response Engineer",
				"strong-password")))
				.willReturn(new UserAccountView(
						42L,
						"new.responder",
						"New Response Engineer",
						Set.of("RESPONDER"),
						new UserAccountView.TeamView(7L, "Incident Response")));

		mockMvc.perform(post("/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "New Response Engineer",
								  "username": "new.responder",
								  "password": "strong-password"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.username").value("new.responder"))
				.andExpect(jsonPath("$.displayName").value("New Response Engineer"))
				.andExpect(jsonPath("$.roles[0]").value("RESPONDER"))
				.andExpect(jsonPath("$.team.id").value(7))
				.andExpect(jsonPath("$.team.name").value("Incident Response"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void invalidRegistrationReturnsFieldErrorsWithoutCallingTheUseCase() throws Exception {
		mockMvc.perform(post("/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "A",
								  "username": "invalid username",
								  "password": "short"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed"))
				.andExpect(jsonPath("$.path").value("/register"))
				.andExpect(jsonPath("$.fieldErrors.displayName")
						.value("Display name must contain between 2 and 150 characters"))
				.andExpect(jsonPath("$.fieldErrors.username")
						.value("Username may contain only letters, numbers, dots, dashes, and underscores"))
				.andExpect(jsonPath("$.fieldErrors.password")
						.value("Password must contain between 8 and 200 characters"));
		verifyNoInteractions(registerUserAccount);
	}

	@Test
	void duplicateUsernameReturnsConflictOnTheUsernameField() throws Exception {
		RegisterUserAccountCommand command = new RegisterUserAccountCommand(
				"new.responder",
				"New Response Engineer",
				"strong-password");
		willThrow(new DuplicateUsernameException()).given(registerUserAccount).execute(command);

		mockMvc.perform(post("/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "New Response Engineer",
								  "username": "new.responder",
								  "password": "strong-password"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("Username is already registered"))
				.andExpect(jsonPath("$.path").value("/register"))
				.andExpect(jsonPath("$.fieldErrors.username")
						.value("Username is already registered"));
	}

	@Test
	void unavailableRegistrationTeamReturnsSanitizedServiceUnavailableError() throws Exception {
		RegisterUserAccountCommand command = new RegisterUserAccountCommand(
				"new.responder",
				"New Response Engineer",
				"strong-password");
		willThrow(new RegistrationUnavailableException())
				.given(registerUserAccount)
				.execute(command);

		mockMvc.perform(post("/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "New Response Engineer",
								  "username": "new.responder",
								  "password": "strong-password"
								}
								"""))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.message")
						.value("Registration is temporarily unavailable"))
				.andExpect(jsonPath("$.path").value("/register"))
				.andExpect(jsonPath("$.fieldErrors").doesNotExist());
	}
}
