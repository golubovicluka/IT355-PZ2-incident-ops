package com.golubovicluka.incident_ops.servicecatalog.web.response;

import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;

public record ManagedServiceResponse(
		Long id,
		String name,
		String description,
		Criticality criticality,
		TeamResponse owningTeam) {

	public static ManagedServiceResponse from(ManagedServiceView service) {
		return new ManagedServiceResponse(
				service.id(),
				service.name(),
				service.description(),
				service.criticality(),
				new TeamResponse(
						service.owningTeam().id(),
						service.owningTeam().name()));
	}

	public record TeamResponse(Long id, String name) {
	}
}
