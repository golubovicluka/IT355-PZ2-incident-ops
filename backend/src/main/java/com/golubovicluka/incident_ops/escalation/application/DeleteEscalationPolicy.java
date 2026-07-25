package com.golubovicluka.incident_ops.escalation.application;

import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicy;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyNotFoundException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteEscalationPolicy {

	private final EscalationPolicyRepository policies;

	public DeleteEscalationPolicy(EscalationPolicyRepository policies) {
		this.policies = policies;
	}

	@Transactional
	public void execute(long id) {
		EscalationPolicy policy = policies.findById(id)
				.orElseThrow(EscalationPolicyNotFoundException::new);
		policies.delete(policy);
	}
}
