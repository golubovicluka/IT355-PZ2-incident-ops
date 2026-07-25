package com.golubovicluka.incident_ops.escalation.application.dto;

import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicy;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;

public record EscalationPolicyView(
		Long id,
		ManagedServiceView managedService,
		IncidentPriority priority,
		long acknowledgementMinutes,
		long resolutionMinutes) {

	public static EscalationPolicyView from(EscalationPolicy policy) {
		return new EscalationPolicyView(
				policy.id(),
				new ManagedServiceView(
						policy.managedService().id(),
						policy.managedService().name()),
				policy.priority(),
				policy.acknowledgementDeadline().toMinutes(),
				policy.resolutionDeadline().toMinutes());
	}

	public record ManagedServiceView(Long id, String name) {
	}
}
