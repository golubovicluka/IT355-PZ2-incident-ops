package com.golubovicluka.incident_ops.escalation.web.response;

import com.golubovicluka.incident_ops.escalation.application.dto.EscalationPolicyView;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;

public record EscalationPolicyResponse(
		Long id,
		ManagedServiceResponse managedService,
		IncidentPriority priority,
		long acknowledgementMinutes,
		long resolutionMinutes) {

	public static EscalationPolicyResponse from(EscalationPolicyView policy) {
		return new EscalationPolicyResponse(
				policy.id(),
				new ManagedServiceResponse(
						policy.managedService().id(),
						policy.managedService().name()),
				policy.priority(),
				policy.acknowledgementMinutes(),
				policy.resolutionMinutes());
	}

	public record ManagedServiceResponse(Long id, String name) {
	}
}
