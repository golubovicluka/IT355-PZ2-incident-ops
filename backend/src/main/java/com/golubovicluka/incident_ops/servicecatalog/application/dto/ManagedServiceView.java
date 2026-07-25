package com.golubovicluka.incident_ops.servicecatalog.application.dto;

import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;

public record ManagedServiceView(
		Long id,
		String name,
		String description,
		Criticality criticality,
		TeamView owningTeam) {

	public static ManagedServiceView from(ManagedService service) {
		return new ManagedServiceView(
				service.id(),
				service.name(),
				service.description(),
				service.criticality(),
				new TeamView(service.owningTeam().id(), service.owningTeam().name()));
	}

	public record TeamView(Long id, String name) {
	}
}
