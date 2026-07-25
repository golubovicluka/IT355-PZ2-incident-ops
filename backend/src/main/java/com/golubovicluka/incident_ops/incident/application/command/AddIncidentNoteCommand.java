package com.golubovicluka.incident_ops.incident.application.command;

public record AddIncidentNoteCommand(
		long id,
		String note,
		String actorUsername) {
}
