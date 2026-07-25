package com.golubovicluka.incident_ops.incident.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.golubovicluka.incident_ops.analytics.domain.SlaEvaluation;
import com.golubovicluka.incident_ops.analytics.domain.SlaPhase;
import com.golubovicluka.incident_ops.incident.application.AddIncidentNote;
import com.golubovicluka.incident_ops.incident.application.CreateIncident;
import com.golubovicluka.incident_ops.incident.application.ChangeIncidentStatus;
import com.golubovicluka.incident_ops.incident.application.GetIncident;
import com.golubovicluka.incident_ops.incident.application.IncidentActorNotFoundException;
import com.golubovicluka.incident_ops.incident.application.IncidentAssigneeNotFoundException;
import com.golubovicluka.incident_ops.incident.application.IncidentManagedServiceNotFoundException;
import com.golubovicluka.incident_ops.incident.application.ListIncidents;
import com.golubovicluka.incident_ops.incident.application.UpdateIncident;
import com.golubovicluka.incident_ops.incident.application.command.AddIncidentNoteCommand;
import com.golubovicluka.incident_ops.incident.application.command.ChangeIncidentStatusCommand;
import com.golubovicluka.incident_ops.incident.application.command.CreateIncidentCommand;
import com.golubovicluka.incident_ops.incident.application.command.UpdateIncidentCommand;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentSlaView;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentSummaryView;
import com.golubovicluka.incident_ops.incident.domain.IncidentCriteria;
import com.golubovicluka.incident_ops.incident.domain.IncidentEventKind;
import com.golubovicluka.incident_ops.incident.domain.IncidentNotFoundException;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import com.golubovicluka.incident_ops.incident.domain.InvalidIncidentStatusTransitionException;
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

@WebMvcTest(IncidentController.class)
@Import({
		ApplicationClockConfiguration.class,
		ApiAccessDeniedHandler.class,
		ApiAuthenticationEntryPoint.class,
		ApiErrorWriter.class,
		ApiExceptionHandler.class,
		JwtConfiguration.class,
		SecurityConfiguration.class,
		IncidentExceptionHandler.class
})
@ActiveProfiles("test")
class IncidentControllerWebMvcTest {

	private static final Instant CREATED_AT =
			Instant.parse("2026-07-25T08:15:30Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreateIncident createIncident;

	@MockitoBean
	private AddIncidentNote addIncidentNote;

	@MockitoBean
	private ChangeIncidentStatus changeIncidentStatus;

	@MockitoBean
	private GetIncident getIncident;

	@MockitoBean
	private ListIncidents listIncidents;

	@MockitoBean
	private UpdateIncident updateIncident;

	@Test
	@WithMockUser(username = "responder", roles = "RESPONDER")
	void createsIncidentUsingAuthenticatedUsernameAndReturnsDetailShape()
			throws Exception {
		CreateIncidentCommand command = new CreateIncidentCommand(
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				7L,
				12L,
				"responder");
		given(createIncident.execute(command)).willReturn(detail());

		mockMvc.perform(post("/api/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Checkout failures",
								  "description": "Card payments are timing out.",
								  "priority": "SEV1",
								  "managedServiceId": 7,
								  "assigneeId": 12,
								  "reporterId": 999,
								  "eventActorId": 998
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string(
						"Location",
						"http://localhost/api/incidents/42"))
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.referenceCode")
						.value("INC-20260725-AB12CD34"))
				.andExpect(jsonPath("$.description")
						.value("Card payments are timing out."))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.managedService.id").value(7))
				.andExpect(jsonPath("$.reporter.username").value("responder"))
				.andExpect(jsonPath("$.assignee.username").value("ana"))
				.andExpect(jsonPath("$.timeline[0].kind").value("CREATED"))
				.andExpect(jsonPath("$.timeline[0].actor.username")
						.value("responder"))
				.andExpect(jsonPath("$.timeline[0].occurredAt")
						.value("2026-07-25T08:15:30Z"))
				.andExpect(jsonPath("$.sla.state")
						.value("NOT_CONFIGURED"))
				.andExpect(jsonPath("$.sla.deadline").doesNotExist());

		verify(createIncident).execute(command);
	}

	@Test
	@WithMockUser(username = "responder", roles = "RESPONDER")
	void listsFilteredSummariesInApplicationOrder() throws Exception {
		IncidentCriteria criteria = new IncidentCriteria(
				IncidentStatus.OPEN,
				IncidentPriority.SEV1,
				7L);
		given(listIncidents.execute(criteria)).willReturn(List.of(summary()));

		mockMvc.perform(get("/api/incidents")
						.queryParam("status", "OPEN")
						.queryParam("priority", "SEV1")
						.queryParam("serviceId", "7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(42))
				.andExpect(jsonPath("$[0].referenceCode")
						.value("INC-20260725-AB12CD34"))
				.andExpect(jsonPath("$[0].managedService.name")
						.value("Payments API"))
				.andExpect(jsonPath("$[0].sla.state")
						.value("ON_TRACK"))
				.andExpect(jsonPath("$[0].sla.phase")
						.value("ACKNOWLEDGEMENT"))
				.andExpect(jsonPath("$[0].sla.deadline")
						.value("2026-07-25T08:25:30Z"))
				.andExpect(jsonPath("$[0].description").doesNotExist())
				.andExpect(jsonPath("$[0].reporter").doesNotExist())
				.andExpect(jsonPath("$[0].timeline").doesNotExist());

		verify(listIncidents).execute(criteria);
	}

	@Test
	@WithMockUser(username = "responder", roles = "RESPONDER")
	void getsDetailAndUpdatesOnlyEditableFields() throws Exception {
		given(getIncident.execute(42L)).willReturn(detail());
		UpdateIncidentCommand command = new UpdateIncidentCommand(
				42L,
				"Updated title",
				"Updated description",
				IncidentPriority.SEV2,
				8L,
				null);
		given(updateIncident.execute(command)).willReturn(detail());

		mockMvc.perform(get("/api/incidents/42"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timeline[0].id").value(99));
		mockMvc.perform(put("/api/incidents/42")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Updated title",
								  "description": "Updated description",
								  "priority": "SEV2",
								  "managedServiceId": 8,
								  "assigneeId": null,
								  "reporterId": 999,
								  "eventActorId": 998
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reporter.username").value("responder"))
				.andExpect(jsonPath("$.timeline[0].actor.username")
						.value("responder"));

		verify(updateIncident).execute(command);
	}

	@Test
	@WithMockUser(username = "responder", roles = "RESPONDER")
	void changesStatusUsingAuthenticatedActorAndReturnsTimelineDetails()
			throws Exception {
		ChangeIncidentStatusCommand command = new ChangeIncidentStatusCommand(
				42L,
				IncidentStatus.ACKNOWLEDGED,
				"responder");
		given(changeIncidentStatus.execute(command))
				.willReturn(acknowledgedDetail());

		mockMvc.perform(put("/api/incidents/42/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "ACKNOWLEDGED",
								  "actorUsername": "attacker"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
				.andExpect(jsonPath("$.acknowledgedAt")
						.value("2026-07-25T08:20:30Z"))
				.andExpect(jsonPath("$.resolvedAt").doesNotExist())
				.andExpect(jsonPath("$.allowedTransitions[0]")
						.value("INVESTIGATING"))
				.andExpect(jsonPath("$.timeline[1].kind")
						.value("STATUS_CHANGED"))
				.andExpect(jsonPath("$.timeline[1].previousStatus")
						.value("OPEN"))
				.andExpect(jsonPath("$.timeline[1].newStatus")
						.value("ACKNOWLEDGED"))
				.andExpect(jsonPath("$.timeline[1].actor.username")
						.value("responder"));

		verify(changeIncidentStatus).execute(command);
	}

	@Test
	@WithMockUser(username = "responder", roles = "RESPONDER")
	void addsNoteUsingAuthenticatedActorAndReturnsUpdatedTimeline()
			throws Exception {
		AddIncidentNoteCommand command = new AddIncidentNoteCommand(
				42L,
				"Rolled back the checkout deployment.",
				"responder");
		given(addIncidentNote.execute(command)).willReturn(notedDetail());

		mockMvc.perform(post("/api/incidents/42/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "note": "Rolled back the checkout deployment.",
								  "actorUsername": "attacker"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.timeline[1].kind")
						.value("NOTE_ADDED"))
				.andExpect(jsonPath("$.timeline[1].note")
						.value("Rolled back the checkout deployment."))
				.andExpect(jsonPath("$.timeline[1].previousStatus")
						.doesNotExist())
				.andExpect(jsonPath("$.timeline[1].newStatus").doesNotExist())
				.andExpect(jsonPath("$.timeline[1].actor.username")
						.value("responder"))
				.andExpect(jsonPath("$.timeline[1].occurredAt")
						.value("2026-07-25T08:20:30Z"));

		verify(addIncidentNote).execute(command);
	}

	@Test
	@WithMockUser(username = "responder", roles = "RESPONDER")
	void rejectsInvalidStatusTransitionWithStableConflict() throws Exception {
		ChangeIncidentStatusCommand command = new ChangeIncidentStatusCommand(
				42L,
				IncidentStatus.CLOSED,
				"responder");
		given(changeIncidentStatus.execute(command))
				.willThrow(new InvalidIncidentStatusTransitionException(
						IncidentStatus.OPEN,
						IncidentStatus.CLOSED));

		mockMvc.perform(put("/api/incidents/42/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status": "CLOSED"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Incident status cannot transition from OPEN to CLOSED"))
				.andExpect(jsonPath("$.path")
						.value("/api/incidents/42/status"));
	}

	@Test
	@WithMockUser(username = "missing", roles = "RESPONDER")
	void validatesStatusRequestAndAuthenticatedActor() throws Exception {
		mockMvc.perform(put("/api/incidents/42/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status": null}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.status")
						.value("Incident status is required"));

		ChangeIncidentStatusCommand command = new ChangeIncidentStatusCommand(
				42L,
				IncidentStatus.ACKNOWLEDGED,
				"missing");
		given(changeIncidentStatus.execute(command))
				.willThrow(new IncidentActorNotFoundException());

		mockMvc.perform(put("/api/incidents/42/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status": "ACKNOWLEDGED"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message")
						.value("Authenticated incident actor does not exist"));
	}

	@Test
	@WithMockUser(username = "responder", roles = "RESPONDER")
	void validatesNoteContentAndUnknownIncident() throws Exception {
		mockMvc.perform(post("/api/incidents/42/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"note": " "}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.note")
						.value("Incident note is required"));

		mockMvc.perform(post("/api/incidents/42/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"note": "%s"}
								""".formatted("x".repeat(2001))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.note")
						.value("Incident note must not exceed 2000 characters"));

		AddIncidentNoteCommand missing = new AddIncidentNoteCommand(
				404L,
				"Investigating.",
				"responder");
		given(addIncidentNote.execute(missing))
				.willThrow(new IncidentNotFoundException());

		mockMvc.perform(post("/api/incidents/404/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"note": "Investigating."}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message")
						.value("Incident does not exist"));
	}

	@Test
	@WithMockUser(username = "responder", roles = "RESPONDER")
	void validatesCreateAndUpdateRequests() throws Exception {
		mockMvc.perform(post("/api/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": " ",
								  "description": " ",
								  "priority": null,
								  "managedServiceId": 0,
								  "assigneeId": -1
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("Request validation failed"))
				.andExpect(jsonPath("$.fieldErrors.title")
						.value("Incident title is required"))
				.andExpect(jsonPath("$.fieldErrors.description")
						.value("Incident description is required"))
				.andExpect(jsonPath("$.fieldErrors.priority")
						.value("Incident priority is required"))
				.andExpect(jsonPath("$.fieldErrors.managedServiceId")
						.value("Managed service must be selected"))
				.andExpect(jsonPath("$.fieldErrors.assigneeId")
						.value("Assignee must be a valid user"));

		mockMvc.perform(put("/api/incidents/42")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Valid title",
								  "description": "Valid description",
								  "priority": "URGENT",
								  "managedServiceId": 7
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("Request body is missing or malformed"));
	}

	@Test
	@WithMockUser(username = "responder", roles = "RESPONDER")
	void validatesFilterValuesWithStableErrors() throws Exception {
		mockMvc.perform(get("/api/incidents")
						.queryParam("priority", "URGENT"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("Request parameter is invalid"))
				.andExpect(jsonPath("$.fieldErrors.priority")
						.value("Unsupported value"));

		mockMvc.perform(get("/api/incidents")
						.queryParam("serviceId", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("Request parameter is invalid"))
				.andExpect(jsonPath("$.fieldErrors.serviceId")
						.value("Managed service must be selected"));
	}

	@Test
	@WithMockUser(username = "responder", roles = "RESPONDER")
	void mapsMissingResourcesToStableErrors() throws Exception {
		CreateIncidentCommand missingService = new CreateIncidentCommand(
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				404L,
				null,
				"responder");
		given(createIncident.execute(missingService))
				.willThrow(new IncidentManagedServiceNotFoundException());
		given(getIncident.execute(404L))
				.willThrow(new IncidentNotFoundException());
		UpdateIncidentCommand missingAssignee = new UpdateIncidentCommand(
				42L,
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				7L,
				404L);
		given(updateIncident.execute(missingAssignee))
				.willThrow(new IncidentAssigneeNotFoundException());

		mockMvc.perform(post("/api/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request("Checkout failures", 404, "null")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.managedServiceId")
						.value("Managed service does not exist"));
		mockMvc.perform(get("/api/incidents/404"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Incident does not exist"));
		mockMvc.perform(put("/api/incidents/42")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request("Checkout failures", 7, "404")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.assigneeId")
						.value("Assignee does not exist"));
	}

	@Test
	void anonymousUserCannotAccessIncidentApi() throws Exception {
		mockMvc.perform(get("/api/incidents")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/incidents/42"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/incidents")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request("Checkout failures", 7, "null")))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/incidents/42")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request("Checkout failures", 7, "null")))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/incidents/42/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status": "ACKNOWLEDGED"}
								"""))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/incidents/42/events")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"note": "Investigating."}
								"""))
				.andExpect(status().isUnauthorized());
	}

	private IncidentSummaryView summary() {
		return new IncidentSummaryView(
				42L,
				"INC-20260725-AB12CD34",
				"Checkout failures",
				IncidentPriority.SEV1,
				IncidentStatus.OPEN,
				new IncidentSummaryView.ManagedServiceView(7L, "Payments API"),
				new IncidentSummaryView.UserView(12L, "ana", "Ana Anić"),
				CREATED_AT,
				CREATED_AT,
				IncidentSlaView.from(SlaEvaluation.onTrack(
						SlaPhase.ACKNOWLEDGEMENT,
						CREATED_AT.plusSeconds(600))));
	}

	private IncidentDetailView detail() {
		IncidentDetailView.UserView reporter =
				new IncidentDetailView.UserView(
						11L,
						"responder",
						"Response Engineer");
		return new IncidentDetailView(
				42L,
				"INC-20260725-AB12CD34",
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				IncidentStatus.OPEN,
				new IncidentDetailView.ManagedServiceView(7L, "Payments API"),
				reporter,
				new IncidentDetailView.UserView(12L, "ana", "Ana Anić"),
				CREATED_AT,
				CREATED_AT,
				null,
				null,
				List.of(IncidentStatus.ACKNOWLEDGED, IncidentStatus.INVESTIGATING),
				List.of(new IncidentDetailView.EventView(
						99L,
						IncidentEventKind.CREATED,
						reporter,
						null,
						null,
						null,
						CREATED_AT)));
	}

	private IncidentDetailView acknowledgedDetail() {
		IncidentDetailView.UserView reporter =
				new IncidentDetailView.UserView(
						11L,
						"responder",
						"Response Engineer");
		Instant acknowledgedAt = CREATED_AT.plusSeconds(300);
		return new IncidentDetailView(
				42L,
				"INC-20260725-AB12CD34",
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				IncidentStatus.ACKNOWLEDGED,
				new IncidentDetailView.ManagedServiceView(7L, "Payments API"),
				reporter,
				new IncidentDetailView.UserView(12L, "ana", "Ana Anić"),
				CREATED_AT,
				acknowledgedAt,
				acknowledgedAt,
				null,
				List.of(IncidentStatus.INVESTIGATING),
				List.of(
						new IncidentDetailView.EventView(
								99L,
								IncidentEventKind.CREATED,
								reporter,
								null,
								null,
								null,
								CREATED_AT),
						new IncidentDetailView.EventView(
								100L,
								IncidentEventKind.STATUS_CHANGED,
								reporter,
								IncidentStatus.OPEN,
								IncidentStatus.ACKNOWLEDGED,
								null,
								acknowledgedAt)));
	}

	private IncidentDetailView notedDetail() {
		IncidentDetailView.UserView reporter =
				new IncidentDetailView.UserView(
						11L,
						"responder",
						"Response Engineer");
		Instant notedAt = CREATED_AT.plusSeconds(300);
		return new IncidentDetailView(
				42L,
				"INC-20260725-AB12CD34",
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				IncidentStatus.OPEN,
				new IncidentDetailView.ManagedServiceView(7L, "Payments API"),
				reporter,
				new IncidentDetailView.UserView(12L, "ana", "Ana Anić"),
				CREATED_AT,
				notedAt,
				null,
				null,
				List.of(
						IncidentStatus.ACKNOWLEDGED,
						IncidentStatus.INVESTIGATING),
				List.of(
						new IncidentDetailView.EventView(
								99L,
								IncidentEventKind.CREATED,
								reporter,
								null,
								null,
								null,
								CREATED_AT),
						new IncidentDetailView.EventView(
								100L,
								IncidentEventKind.NOTE_ADDED,
								reporter,
								null,
								null,
								"Rolled back the checkout deployment.",
								notedAt)));
	}

	private String request(String title, long serviceId, String assigneeId) {
		return """
				{
				  "title": "%s",
				  "description": "Card payments are timing out.",
				  "priority": "SEV1",
				  "managedServiceId": %d,
				  "assigneeId": %s
				}
				""".formatted(title, serviceId, assigneeId);
	}
}
