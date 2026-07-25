package com.golubovicluka.incident_ops.servicecatalog.application.command;

import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;

public record UpdateManagedServiceCommand(
		long id,
		String name,
		String description,
		Criticality criticality,
		long owningTeamId) {
}
