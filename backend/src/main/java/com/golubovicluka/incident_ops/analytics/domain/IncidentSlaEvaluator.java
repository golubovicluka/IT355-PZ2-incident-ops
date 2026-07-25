package com.golubovicluka.incident_ops.analytics.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;

public final class IncidentSlaEvaluator {

	public SlaEvaluation evaluate(
			IncidentStatus status,
			Instant createdAt,
			Instant acknowledgedAt,
			Instant resolvedAt,
			Duration acknowledgementTarget,
			Duration resolutionTarget,
			Instant now) {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(now, "now must not be null");
		if (acknowledgementTarget == null || resolutionTarget == null) {
			return SlaEvaluation.notConfigured();
		}

		Instant acknowledgementDeadline =
				createdAt.plus(acknowledgementTarget);
		Instant resolutionDeadline = createdAt.plus(resolutionTarget);

		if (status == IncidentStatus.OPEN) {
			return now.isAfter(acknowledgementDeadline)
					? SlaEvaluation.breached(
							SlaPhase.ACKNOWLEDGEMENT,
							acknowledgementDeadline)
					: SlaEvaluation.onTrack(
							SlaPhase.ACKNOWLEDGEMENT,
							acknowledgementDeadline);
		}
		if (acknowledgedAt == null) {
			throw new IllegalArgumentException(
					"non-open incident must have acknowledgement time");
		}
		if (acknowledgedAt.isAfter(acknowledgementDeadline)) {
			return SlaEvaluation.breached(
					SlaPhase.ACKNOWLEDGEMENT,
					acknowledgementDeadline);
		}

		if (status == IncidentStatus.ACKNOWLEDGED
				|| status == IncidentStatus.INVESTIGATING) {
			return now.isAfter(resolutionDeadline)
					? SlaEvaluation.breached(
							SlaPhase.RESOLUTION,
							resolutionDeadline)
					: SlaEvaluation.onTrack(
							SlaPhase.RESOLUTION,
							resolutionDeadline);
		}
		if (resolvedAt == null) {
			throw new IllegalArgumentException(
					"resolved or closed incident must have resolution time");
		}
		return resolvedAt.isAfter(resolutionDeadline)
				? SlaEvaluation.breached(
						SlaPhase.RESOLUTION,
						resolutionDeadline)
				: SlaEvaluation.met(
						SlaPhase.RESOLUTION,
						resolutionDeadline);
	}
}
