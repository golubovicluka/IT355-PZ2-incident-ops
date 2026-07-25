package com.golubovicluka.incident_ops.escalation.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.golubovicluka.incident_ops.escalation.application.EscalateIncident;
import com.golubovicluka.incident_ops.escalation.application.command.EscalateIncidentCommand;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.IncidentEscalationNotAllowedException;
import com.golubovicluka.incident_ops.incident.domain.IncidentEventKind;
import com.golubovicluka.incident_ops.incident.domain.IncidentNotFoundException;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
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

@WebMvcTest(IncidentEscalationController.class)
@Import({
		ApplicationClockConfiguration.class,
		ApiAccessDeniedHandler.class,
		ApiAuthenticationEntryPoint.class,
		ApiErrorWriter.class,
		ApiExceptionHandler.class,
		JwtConfiguration.class,
		SecurityConfiguration.class,
		IncidentEscalationExceptionHandler.class
})
@ActiveProfiles("test")
class IncidentEscalationControllerWebMvcTest {

	private static final Instant ESCALATED_AT =
			Instant.parse("2026-07-25T08:20:30Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EscalateIncident escalateIncident;

	@Test
	@WithMockUser(username = "responder", roles = "RESPONDER")
	void escalatesUsingAuthenticatedActorAndReturnsHistoryAndTimeline()
			throws Exception {
		EscalateIncidentCommand command = new EscalateIncidentCommand(
				42L,
				"Checkout is unavailable.",
				"responder");
		given(escalateIncident.execute(command)).willReturn(detail());

		mockMvc.perform(post("/api/incidents/42/escalations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "reason": "Checkout is unavailable.",
								  "level": 99,
								  "actorUsername": "attacker"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.escalations[0].level").value(1))
				.andExpect(jsonPath("$.escalations[0].reason")
						.value("Checkout is unavailable."))
				.andExpect(jsonPath("$.escalations[0].actor.username")
						.value("responder"))
				.andExpect(jsonPath("$.escalations[0].escalatedAt")
						.value("2026-07-25T08:20:30Z"))
				.andExpect(jsonPath("$.timeline[0].kind").value("ESCALATED"))
				.andExpect(jsonPath("$.timeline[0].escalationLevel").value(1))
				.andExpect(jsonPath("$.timeline[0].escalationReason")
						.value("Checkout is unavailable."));

		verify(escalateIncident).execute(command);
	}

	@Test
	@WithMockUser(username = "responder", roles = "RESPONDER")
	void validatesReasonAndMapsUnknownOrBlockedIncident() throws Exception {
		mockMvc.perform(post("/api/incidents/42/escalations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reason": " "}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.reason")
						.value("Escalation reason is required"));
		mockMvc.perform(post("/api/incidents/42/escalations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reason": "%s"}
								""".formatted("x".repeat(1001))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.reason")
						.value("Escalation reason must not exceed 1000 characters"));

		given(escalateIncident.execute(new EscalateIncidentCommand(
				404L,
				"Investigate customer impact.",
				"responder"))).willThrow(new IncidentNotFoundException());
		given(escalateIncident.execute(new EscalateIncidentCommand(
				42L,
				"Investigate customer impact.",
				"responder"))).willThrow(
						new IncidentEscalationNotAllowedException(
								IncidentStatus.RESOLVED));
		given(escalateIncident.execute(new EscalateIncidentCommand(
				43L,
				"Investigate customer impact.",
				"responder"))).willThrow(
						new IncidentEscalationNotAllowedException(
								IncidentStatus.CLOSED));

		mockMvc.perform(post("/api/incidents/404/escalations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reason": "Investigate customer impact."}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message")
						.value("Incident does not exist"));
		mockMvc.perform(post("/api/incidents/42/escalations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reason": "Investigate customer impact."}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Incident cannot be escalated while its status is RESOLVED"));
		mockMvc.perform(post("/api/incidents/43/escalations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reason": "Investigate customer impact."}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Incident cannot be escalated while its status is CLOSED"));
	}

	@Test
	void anonymousUserCannotEscalateIncident() throws Exception {
		mockMvc.perform(post("/api/incidents/42/escalations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reason": "Investigate customer impact."}
								"""))
				.andExpect(status().isUnauthorized());
	}

	private IncidentDetailView detail() {
		IncidentDetailView.UserView actor =
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
				actor,
				null,
				ESCALATED_AT.minusSeconds(300),
				ESCALATED_AT,
				null,
				null,
				List.of(IncidentStatus.ACKNOWLEDGED),
				List.of(new IncidentDetailView.EventView(
						100L,
						IncidentEventKind.ESCALATED,
						actor,
						null,
						null,
						null,
						1,
						"Checkout is unavailable.",
						ESCALATED_AT)));
	}
}
