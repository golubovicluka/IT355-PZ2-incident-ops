package com.golubovicluka.incident_ops.escalation.web.request;

import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EscalationPolicyRequest(
		@NotNull(message = "Managed service must be selected")
		@Positive(message = "Managed service must be selected")
		Long managedServiceId,
		@NotNull(message = "Incident priority must be selected")
		IncidentPriority priority,
		@NotNull(message = "Acknowledgement deadline is required")
		@Positive(message = "Acknowledgement deadline must be positive")
		Long acknowledgementMinutes,
		@NotNull(message = "Resolution deadline is required")
		@Positive(message = "Resolution deadline must be positive")
		Long resolutionMinutes) {
}
