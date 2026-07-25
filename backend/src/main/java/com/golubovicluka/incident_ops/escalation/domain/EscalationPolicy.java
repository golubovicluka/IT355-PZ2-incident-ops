package com.golubovicluka.incident_ops.escalation.domain;

import java.time.Duration;
import java.util.Objects;

import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;

public record EscalationPolicy(
		Long id,
		PolicyManagedService managedService,
		IncidentPriority priority,
		Duration acknowledgementDeadline,
		Duration resolutionDeadline) {

	public EscalationPolicy {
		managedService = Objects.requireNonNull(
				managedService,
				"managedService must not be null");
		priority = Objects.requireNonNull(priority, "priority must not be null");
		validateDeadline(
				acknowledgementDeadline,
				"acknowledgementMinutes",
				"Acknowledgement");
		validateDeadline(
				resolutionDeadline,
				"resolutionMinutes",
				"Resolution");
		if (acknowledgementDeadline.compareTo(resolutionDeadline) > 0) {
			throw new InvalidEscalationPolicyDeadlineException(
					"acknowledgementMinutes",
					"Acknowledgement deadline must not exceed the resolution deadline");
		}
	}

	public static EscalationPolicy create(
			PolicyManagedService managedService,
			IncidentPriority priority,
			Duration acknowledgementDeadline,
			Duration resolutionDeadline) {
		return new EscalationPolicy(
				null,
				managedService,
				priority,
				acknowledgementDeadline,
				resolutionDeadline);
	}

	public EscalationPolicy update(
			PolicyManagedService managedService,
			IncidentPriority priority,
			Duration acknowledgementDeadline,
			Duration resolutionDeadline) {
		return new EscalationPolicy(
				id,
				managedService,
				priority,
				acknowledgementDeadline,
				resolutionDeadline);
	}

	private static void validateDeadline(
			Duration deadline,
			String field,
			String label) {
		if (deadline == null || deadline.isZero() || deadline.isNegative()) {
			throw new InvalidEscalationPolicyDeadlineException(
					field,
					label + " deadline must be positive");
		}
		if (deadline.getNano() != 0 || deadline.getSeconds() % 60 != 0) {
			throw new InvalidEscalationPolicyDeadlineException(
					field,
					label + " deadline must use whole minutes");
		}
	}
}
