package com.golubovicluka.incident_ops.analytics.domain;

import java.time.Instant;

public record SlaEvaluation(
		SlaState state,
		SlaPhase phase,
		Instant deadline) {

	public static SlaEvaluation notConfigured() {
		return new SlaEvaluation(SlaState.NOT_CONFIGURED, null, null);
	}

	public static SlaEvaluation onTrack(
			SlaPhase phase,
			Instant deadline) {
		return new SlaEvaluation(SlaState.ON_TRACK, phase, deadline);
	}

	public static SlaEvaluation breached(
			SlaPhase phase,
			Instant deadline) {
		return new SlaEvaluation(SlaState.BREACHED, phase, deadline);
	}

	public static SlaEvaluation met(
			SlaPhase phase,
			Instant deadline) {
		return new SlaEvaluation(SlaState.MET, phase, deadline);
	}
}
