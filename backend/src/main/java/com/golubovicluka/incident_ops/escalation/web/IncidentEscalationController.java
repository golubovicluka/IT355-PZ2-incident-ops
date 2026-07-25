package com.golubovicluka.incident_ops.escalation.web;

import com.golubovicluka.incident_ops.escalation.application.EscalateIncident;
import com.golubovicluka.incident_ops.escalation.application.command.EscalateIncidentCommand;
import com.golubovicluka.incident_ops.escalation.web.request.IncidentEscalationRequest;
import com.golubovicluka.incident_ops.escalation.web.response.EscalatedIncidentResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents/{incidentId}/escalations")
public class IncidentEscalationController {

	private final EscalateIncident escalateIncident;

	public IncidentEscalationController(EscalateIncident escalateIncident) {
		this.escalateIncident = escalateIncident;
	}

	@PostMapping
	EscalatedIncidentResponse escalate(
			@PathVariable long incidentId,
			@Valid @RequestBody IncidentEscalationRequest request,
			Authentication authentication) {
		return EscalatedIncidentResponse.from(escalateIncident.execute(
				new EscalateIncidentCommand(
						incidentId,
						request.reason(),
						authentication.getName())));
	}
}
