package com.golubovicluka.incident_ops.servicecatalog.application.command;

import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;

public record CreateManagedServiceCommand(
		String name,
		String description,
		Criticality criticality,
		long owningTeamId) {
}
