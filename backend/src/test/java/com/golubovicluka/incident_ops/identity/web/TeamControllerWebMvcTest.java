package com.golubovicluka.incident_ops.identity.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.golubovicluka.incident_ops.identity.application.CreateTeam;
import com.golubovicluka.incident_ops.identity.application.DeleteTeam;
import com.golubovicluka.incident_ops.identity.application.ListTeams;
import com.golubovicluka.incident_ops.identity.application.UpdateTeam;
import com.golubovicluka.incident_ops.identity.application.command.CreateTeamCommand;
import com.golubovicluka.incident_ops.identity.application.command.UpdateTeamCommand;
import com.golubovicluka.incident_ops.identity.application.dto.TeamView;
import com.golubovicluka.incident_ops.identity.domain.DuplicateTeamNameException;
import com.golubovicluka.incident_ops.identity.domain.TeamInUseException;
import com.golubovicluka.incident_ops.identity.domain.TeamNotFoundException;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TeamController.class)
@Import({
		ApplicationClockConfiguration.class,
		ApiAccessDeniedHandler.class,
		ApiAuthenticationEntryPoint.class,
		ApiErrorWriter.class,
		ApiExceptionHandler.class,
		JwtConfiguration.class,
		SecurityConfiguration.class,
		TeamExceptionHandler.class
})
@ActiveProfiles("test")
class TeamControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListTeams listTeams;

	@MockitoBean
	private CreateTeam createTeam;

	@MockitoBean
	private UpdateTeam updateTeam;

	@MockitoBean
	private DeleteTeam deleteTeam;

	@Test
	@WithMockUser(roles = "ADMIN")
	void administratorListsTeams() throws Exception {
		given(listTeams.execute()).willReturn(List.of(
				new TeamView(1L, "Administration"),
				new TeamView(2L, "Incident Response")));

		mockMvc.perform(get("/api/admin/teams"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].name").value("Administration"))
				.andExpect(jsonPath("$[1].id").value(2))
				.andExpect(jsonPath("$[1].name").value("Incident Response"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void administratorCreatesTeam() throws Exception {
		given(createTeam.execute(new CreateTeamCommand("Platform Operations")))
				.willReturn(new TeamView(42L, "Platform Operations"));

		mockMvc.perform(post("/api/admin/teams")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Platform Operations"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string(
						"Location",
						"http://localhost/api/admin/teams/42"))
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.name").value("Platform Operations"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void administratorUpdatesTeam() throws Exception {
		given(updateTeam.execute(new UpdateTeamCommand(42L, "Core Platform")))
				.willReturn(new TeamView(42L, "Core Platform"));

		mockMvc.perform(put("/api/admin/teams/42")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Core Platform"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.name").value("Core Platform"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void administratorDeletesTeam() throws Exception {
		mockMvc.perform(delete("/api/admin/teams/42"))
				.andExpect(status().isNoContent());

		verify(deleteTeam).execute(42L);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void blankNameReturnsFieldError() throws Exception {
		mockMvc.perform(post("/api/admin/teams")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "   "
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Request validation failed"))
				.andExpect(jsonPath("$.path").value("/api/admin/teams"))
				.andExpect(jsonPath("$.fieldErrors.name").value("Team name is required"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void overlongNameReturnsFieldError() throws Exception {
		mockMvc.perform(post("/api/admin/teams")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "%s"
								}
								""".formatted("a".repeat(101))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.name")
						.value("Team name must not exceed 100 characters"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void duplicateNameReturnsStableConflict() throws Exception {
		given(createTeam.execute(new CreateTeamCommand("Incident Response")))
				.willThrow(new DuplicateTeamNameException());

		mockMvc.perform(post("/api/admin/teams")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Incident Response"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message")
						.value("A team with this name already exists"))
				.andExpect(jsonPath("$.path").value("/api/admin/teams"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void missingTeamReturnsNotFound() throws Exception {
		given(updateTeam.execute(new UpdateTeamCommand(404L, "Missing")))
				.willThrow(new TeamNotFoundException());

		mockMvc.perform(put("/api/admin/teams/404")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Missing"
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Team was not found"))
				.andExpect(jsonPath("$.path").value("/api/admin/teams/404"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void referencedTeamReturnsStableConflict() throws Exception {
		willThrow(new TeamInUseException()).given(deleteTeam).execute(42L);

		mockMvc.perform(delete("/api/admin/teams/42"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message")
						.value("Team cannot be deleted while users or services reference it"))
				.andExpect(jsonPath("$.path").value("/api/admin/teams/42"));
	}

	@Test
	void anonymousUserCannotReachTeamAdministration() throws Exception {
		mockMvc.perform(get("/api/admin/teams"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.path").value("/api/admin/teams"));
	}

	@Test
	@WithMockUser(roles = "RESPONDER")
	void responderCannotReachTeamAdministration() throws Exception {
		mockMvc.perform(get("/api/admin/teams"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.path").value("/api/admin/teams"));
	}
}
