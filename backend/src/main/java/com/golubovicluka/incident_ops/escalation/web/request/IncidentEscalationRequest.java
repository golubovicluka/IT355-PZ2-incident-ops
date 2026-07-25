package com.golubovicluka.incident_ops.escalation.web.request;

import com.golubovicluka.incident_ops.incident.domain.IncidentEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IncidentEscalationRequest(
		@NotBlank(message = "Escalation reason is required")
		@Size(
				max = IncidentEvent.MAX_ESCALATION_REASON_LENGTH,
				message = "Escalation reason must not exceed 1000 characters")
		String reason) {
}
