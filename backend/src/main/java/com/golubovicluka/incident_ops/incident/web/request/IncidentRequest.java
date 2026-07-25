package com.golubovicluka.incident_ops.incident.web.request;

import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record IncidentRequest(
		@NotBlank(message = "Incident title is required")
		@Size(
				max = Incident.MAX_TITLE_LENGTH,
				message = "Incident title must not exceed 200 characters")
		String title,
		@NotBlank(message = "Incident description is required")
		@Size(
				max = Incident.MAX_DESCRIPTION_LENGTH,
				message = "Incident description must not exceed 4000 characters")
		String description,
		@NotNull(message = "Incident priority is required")
		IncidentPriority priority,
		@NotNull(message = "Managed service must be selected")
		@Positive(message = "Managed service must be selected")
		Long managedServiceId,
		@Positive(message = "Assignee must be a valid user")
		Long assigneeId) {
}
