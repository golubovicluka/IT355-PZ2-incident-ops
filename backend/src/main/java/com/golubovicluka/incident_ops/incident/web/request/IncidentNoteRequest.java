package com.golubovicluka.incident_ops.incident.web.request;

import com.golubovicluka.incident_ops.incident.domain.IncidentEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IncidentNoteRequest(
		@NotBlank(message = "Incident note is required")
		@Size(
				max = IncidentEvent.MAX_NOTE_LENGTH,
				message = "Incident note must not exceed 2000 characters")
		String note) {
}
