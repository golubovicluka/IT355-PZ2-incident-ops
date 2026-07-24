package com.golubovicluka.incident_ops.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.golubovicluka.incident_ops.identity.application.InitializeLocalIdentityData;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import com.golubovicluka.incident_ops.integration.PostgreSQLContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TeamAdministrationIntegrationTest extends PostgreSQLContainerSupport {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private InitializeLocalIdentityData initializeLocalIdentityData;

	@Autowired
	private TeamRepository teams;

	@BeforeEach
	void initializeUsers() {
		initializeLocalIdentityData.initialize();
	}

	@Test
	void administratorCompletesTeamCrudThroughTheApi() throws Exception {
		String token = login("admin", "admin-demo-password");

		MvcResult createdResult = mockMvc.perform(post("/api/admin/teams")
						.header(HttpHeaders.AUTHORIZATION, bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Platform Operations"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Platform Operations"))
				.andReturn();
		long teamId = JSON.readTree(createdResult.getResponse().getContentAsString())
				.get("id")
				.asLong();

		mockMvc.perform(put("/api/admin/teams/{id}", teamId)
						.header(HttpHeaders.AUTHORIZATION, bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Core Platform"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(teamId))
				.andExpect(jsonPath("$.name").value("Core Platform"));

		mockMvc.perform(delete("/api/admin/teams/{id}", teamId)
						.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isNoContent());

		MvcResult listResult = mockMvc.perform(get("/api/admin/teams")
						.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode listedTeams = JSON.readTree(listResult.getResponse().getContentAsString());
		assertThat(listedTeams)
				.noneMatch(team -> team.get("id").asLong() == teamId);
	}

	@Test
	void duplicateNameReturnsConflictFromRealPersistence() throws Exception {
		String token = login("admin", "admin-demo-password");
		String request = """
				{
				  "name": "Platform Operations"
				}
				""";

		mockMvc.perform(post("/api/admin/teams")
						.header(HttpHeaders.AUTHORIZATION, bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/admin/teams")
						.header(HttpHeaders.AUTHORIZATION, bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("A team with this name already exists"));
	}

	@Test
	void referencedDeletionReturnsConflictAndLeavesTeamPersisted() throws Exception {
		String token = login("admin", "admin-demo-password");
		Team administration = teams.findByName("Administration").orElseThrow();

		mockMvc.perform(delete("/api/admin/teams/{id}", administration.id())
						.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Team cannot be deleted while users or services reference it"));

		assertThat(teams.findById(administration.id())).contains(administration);
	}

	@Test
	void realJwtSecuritySeparatesAnonymousResponderAndAdministrator() throws Exception {
		mockMvc.perform(get("/api/admin/teams"))
				.andExpect(status().isUnauthorized());

		String responderToken = login("responder", "responder-demo-password");
		mockMvc.perform(get("/api/admin/teams")
						.header(HttpHeaders.AUTHORIZATION, bearer(responderToken)))
				.andExpect(status().isForbidden());

		String administratorToken = login("admin", "admin-demo-password");
		mockMvc.perform(get("/api/admin/teams")
						.header(HttpHeaders.AUTHORIZATION, bearer(administratorToken)))
				.andExpect(status().isOk());
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
		return JSON.readTree(result.getResponse().getContentAsString())
				.get("token")
				.asString();
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}
}
