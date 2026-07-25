package com.golubovicluka.incident_ops.escalation.application;

import com.golubovicluka.incident_ops.escalation.application.command.CreateEscalationPolicyCommand;
import com.golubovicluka.incident_ops.escalation.application.dto.EscalationPolicyView;
import com.golubovicluka.incident_ops.escalation.domain.DuplicateEscalationPolicyException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicy;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyRepository;
import com.golubovicluka.incident_ops.escalation.domain.PolicyManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.FindManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateEscalationPolicy {

	private final EscalationPolicyRepository policies;
	private final FindManagedService findManagedService;

	public CreateEscalationPolicy(
			EscalationPolicyRepository policies,
			FindManagedService findManagedService) {
		this.policies = policies;
		this.findManagedService = findManagedService;
	}

	@Transactional
	public EscalationPolicyView execute(CreateEscalationPolicyCommand command) {
		ManagedServiceView service = findManagedService
				.execute(command.managedServiceId())
				.orElseThrow(PolicyManagedServiceNotFoundException::new);
		EscalationPolicy policy = EscalationPolicy.create(
				new PolicyManagedService(service.id(), service.name()),
				command.priority(),
				command.acknowledgementDeadline(),
				command.resolutionDeadline());
		if (policies.findByManagedServiceIdAndPriority(
				service.id(),
				policy.priority()).isPresent()) {
			throw new DuplicateEscalationPolicyException();
		}
		return EscalationPolicyView.from(policies.save(policy));
	}
}
