package com.golubovicluka.incident_ops.incident.web.request;

import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import jakarta.validation.constraints.NotNull;

public record IncidentStatusRequest(
		@NotNull(message = "Incident status is required")
		IncidentStatus status) {
}
