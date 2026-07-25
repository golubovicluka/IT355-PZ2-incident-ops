package com.golubovicluka.incident_ops.escalation.application;

import java.util.List;

import com.golubovicluka.incident_ops.escalation.application.dto.EscalationPolicyView;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListEscalationPolicies {

	private final EscalationPolicyRepository policies;

	public ListEscalationPolicies(EscalationPolicyRepository policies) {
		this.policies = policies;
	}

	@Transactional(readOnly = true)
	public List<EscalationPolicyView> execute() {
		return policies.findAll().stream()
				.map(EscalationPolicyView::from)
				.toList();
	}
}
