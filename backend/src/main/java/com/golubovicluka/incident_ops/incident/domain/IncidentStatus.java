package com.golubovicluka.incident_ops.incident.domain;

import java.util.List;

public enum IncidentStatus {
	OPEN,
	ACKNOWLEDGED,
	INVESTIGATING,
	RESOLVED,
	CLOSED;

	public List<IncidentStatus> allowedTransitions() {
		return switch (this) {
			case OPEN -> List.of(ACKNOWLEDGED, INVESTIGATING);
			case ACKNOWLEDGED -> List.of(INVESTIGATING);
			case INVESTIGATING -> List.of(RESOLVED);
			case RESOLVED -> List.of(CLOSED, INVESTIGATING);
			case CLOSED -> List.of();
		};
	}
}
