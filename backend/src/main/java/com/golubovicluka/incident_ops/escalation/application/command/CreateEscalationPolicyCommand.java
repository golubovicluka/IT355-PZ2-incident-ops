package com.golubovicluka.incident_ops.escalation.application.command;

import java.time.Duration;

import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;

public record CreateEscalationPolicyCommand(
		long managedServiceId,
		IncidentPriority priority,
		Duration acknowledgementDeadline,
		Duration resolutionDeadline) {
}
