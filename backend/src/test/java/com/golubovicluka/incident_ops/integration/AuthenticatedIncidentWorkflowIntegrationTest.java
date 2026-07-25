package com.golubovicluka.incident_ops.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicy;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyRepository;
import com.golubovicluka.incident_ops.escalation.domain.EscalationRepository;
import com.golubovicluka.incident_ops.escalation.domain.PolicyManagedService;
import com.golubovicluka.incident_ops.identity.application.InitializeLocalIdentityData;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import com.golubovicluka.incident_ops.servicecatalog.domain.OwningTeam;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthenticatedIncidentWorkflowIntegrationTest
		extends PostgreSQLContainerSupport {

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final String RESPONDER_PASSWORD = "responder-demo-password";
	private static final String ADMINISTRATOR_PASSWORD = "admin-demo-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private InitializeLocalIdentityData initializeLocalIdentityData;

	@Autowired
	private UserAccountRepository users;

	@Autowired
	private ManagedServiceRepository services;

	@Autowired
	private EscalationPolicyRepository policies;

	@Autowired
	private IncidentRepository incidents;

	@Autowired
	private EscalationRepository escalations;

	@BeforeEach
	void initializeWorkflowFixture() {
		initializeLocalIdentityData.initialize();
	}

	@Test
	void authenticatedUsersCompleteTheIncidentDefenseWorkflow() throws Exception {
		UserAccount responder = users.findByUsername("responder").orElseThrow();
		ManagedService service = services.save(ManagedService.create(
				"Defense Workflow API",
				"Authenticated workflow verification service.",
				Criticality.HIGH,
				new OwningTeam(
						responder.team().id(),
						responder.team().name())));
		policies.save(EscalationPolicy.create(
				new PolicyManagedService(service.id(), service.name()),
				IncidentPriority.SEV2,
				Duration.ofMinutes(30),
				Duration.ofHours(4)));

		String responderToken = login("responder", RESPONDER_PASSWORD);
		MvcResult createdResult = mockMvc.perform(post("/api/incidents")
						.header(HttpHeaders.AUTHORIZATION, bearer(responderToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Checkout latency spike",
								  "description": "Elevated latency is affecting checkout requests.",
								  "priority": "SEV2",
								  "managedServiceId": %d,
								  "assigneeId": %d
								}
								""".formatted(service.id(), responder.id())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.reporter.username").value("responder"))
				.andExpect(jsonPath("$.timeline[0].kind").value("CREATED"))
				.andExpect(jsonPath("$.timeline[0].actor.username").value("responder"))
				.andExpect(jsonPath("$.sla.state").value("ON_TRACK"))
				.andExpect(jsonPath("$.sla.phase").value("ACKNOWLEDGEMENT"))
				.andReturn();
		long incidentId = body(createdResult).get("id").asLong();

		mockMvc.perform(put("/api/incidents/{id}/status", incidentId)
						.header(HttpHeaders.AUTHORIZATION, bearer(responderToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "INVESTIGATING"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INVESTIGATING"))
				.andExpect(jsonPath("$.timeline[1].kind").value("STATUS_CHANGED"))
				.andExpect(jsonPath("$.timeline[1].previousStatus").value("OPEN"))
				.andExpect(jsonPath("$.timeline[1].newStatus")
						.value("INVESTIGATING"))
				.andExpect(jsonPath("$.timeline[1].actor.username")
						.value("responder"))
				.andExpect(jsonPath("$.sla.phase").value("RESOLUTION"));

		mockMvc.perform(post("/api/incidents/{id}/events", incidentId)
						.header(HttpHeaders.AUTHORIZATION, bearer(responderToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "note": "Database saturation confirmed; mitigation is running."
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timeline[2].kind").value("NOTE_ADDED"))
				.andExpect(jsonPath("$.timeline[2].note")
						.value("Database saturation confirmed; mitigation is running."))
				.andExpect(jsonPath("$.timeline[2].actor.username")
						.value("responder"));

		mockMvc.perform(post("/api/incidents/{id}/escalations", incidentId)
						.header(HttpHeaders.AUTHORIZATION, bearer(responderToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reason": "Customer impact is expanding across regions."
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timeline[3].kind").value("ESCALATED"))
				.andExpect(jsonPath("$.timeline[3].escalationLevel").value(1))
				.andExpect(jsonPath("$.timeline[3].escalationReason")
						.value("Customer impact is expanding across regions."))
				.andExpect(jsonPath("$.escalations[0].level").value(1))
				.andExpect(jsonPath("$.escalations[0].actor.username")
						.value("responder"));

		mockMvc.perform(post("/api/admin/teams")
						.header(HttpHeaders.AUTHORIZATION, bearer(responderToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Defense Review"
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.path").value("/api/admin/teams"));

		String administratorToken = login("admin", ADMINISTRATOR_PASSWORD);
		MvcResult teamResult = mockMvc.perform(post("/api/admin/teams")
						.header(HttpHeaders.AUTHORIZATION, bearer(administratorToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Defense Review"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Defense Review"))
				.andReturn();
		long teamId = body(teamResult).get("id").asLong();

		mockMvc.perform(put("/api/admin/teams/{id}", teamId)
						.header(HttpHeaders.AUTHORIZATION, bearer(administratorToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Defense Verification"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Defense Verification"));

		mockMvc.perform(delete("/api/admin/teams/{id}", teamId)
						.header(HttpHeaders.AUTHORIZATION, bearer(administratorToken)))
				.andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/incidents/{id}", incidentId)
						.header(HttpHeaders.AUTHORIZATION, bearer(responderToken)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));

		assertThat(incidents.findById(incidentId)).isPresent();
		assertThat(escalations.findHighestLevel(incidentId)).isEqualTo(1);

		mockMvc.perform(delete("/api/incidents/{id}", incidentId)
						.header(HttpHeaders.AUTHORIZATION, bearer(administratorToken)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/incidents/{id}", incidentId)
						.header(HttpHeaders.AUTHORIZATION, bearer(administratorToken)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.path")
						.value("/api/incidents/" + incidentId));

		assertThat(incidents.findById(incidentId)).isEmpty();
		assertThat(escalations.findHighestLevel(incidentId)).isZero();
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
				.andExpect(jsonPath("$.username").value(username))
				.andReturn();
		return body(result).get("token").asString();
	}

	private JsonNode body(MvcResult result) throws Exception {
		return JSON.readTree(result.getResponse().getContentAsString());
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}
}
