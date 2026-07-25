package com.golubovicluka.incident_ops.escalation.application.command;

public record EscalateIncidentCommand(
		long incidentId,
		String reason,
		String actorUsername) {
}
