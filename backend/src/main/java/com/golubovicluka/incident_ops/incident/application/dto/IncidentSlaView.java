package com.golubovicluka.incident_ops.incident.application.dto;

import java.time.Instant;

import com.golubovicluka.incident_ops.analytics.domain.SlaEvaluation;
import com.golubovicluka.incident_ops.analytics.domain.SlaPhase;
import com.golubovicluka.incident_ops.analytics.domain.SlaState;

public record IncidentSlaView(
		SlaState state,
		SlaPhase phase,
		Instant deadline) {

	public static IncidentSlaView from(SlaEvaluation evaluation) {
		return new IncidentSlaView(
				evaluation.state(),
				evaluation.phase(),
				evaluation.deadline());
	}

	public static IncidentSlaView notConfigured() {
		return from(SlaEvaluation.notConfigured());
	}
}
