package com.golubovicluka.incident_ops.escalation.application;

import java.util.Optional;

import com.golubovicluka.incident_ops.escalation.application.dto.EscalationPolicyView;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyRepository;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindEscalationPolicy {

	private final EscalationPolicyRepository policies;

	public FindEscalationPolicy(EscalationPolicyRepository policies) {
		this.policies = policies;
	}

	@Transactional(readOnly = true)
	public Optional<EscalationPolicyView> execute(
			long managedServiceId,
			IncidentPriority priority) {
		return policies.findByManagedServiceIdAndPriority(
				managedServiceId,
				priority).map(EscalationPolicyView::from);
	}
}
