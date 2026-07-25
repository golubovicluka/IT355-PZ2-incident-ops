package com.golubovicluka.incident_ops.servicecatalog.web.request;

import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ManagedServiceRequest(
		@NotBlank(message = "Managed service name is required")
		@Size(
				max = ManagedService.MAX_NAME_LENGTH,
				message = "Managed service name must not exceed 100 characters")
		String name,
		@NotBlank(message = "Managed service description is required")
		@Size(
				max = ManagedService.MAX_DESCRIPTION_LENGTH,
				message = "Managed service description must not exceed 500 characters")
		String description,
		@NotNull(message = "Criticality is required")
		Criticality criticality,
		@NotNull(message = "Owning team must be selected")
		@Positive(message = "Owning team must be selected")
		Long owningTeamId) {
}
