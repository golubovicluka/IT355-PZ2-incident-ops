package com.golubovicluka.incident_ops.escalation.application;

import java.util.Objects;

import com.golubovicluka.incident_ops.escalation.application.command.UpdateEscalationPolicyCommand;
import com.golubovicluka.incident_ops.escalation.application.dto.EscalationPolicyView;
import com.golubovicluka.incident_ops.escalation.domain.DuplicateEscalationPolicyException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicy;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyNotFoundException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyRepository;
import com.golubovicluka.incident_ops.escalation.domain.PolicyManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.FindManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateEscalationPolicy {

	private final EscalationPolicyRepository policies;
	private final FindManagedService findManagedService;

	public UpdateEscalationPolicy(
			EscalationPolicyRepository policies,
			FindManagedService findManagedService) {
		this.policies = policies;
		this.findManagedService = findManagedService;
	}

	@Transactional
	public EscalationPolicyView execute(UpdateEscalationPolicyCommand command) {
		EscalationPolicy existing = policies.findById(command.id())
				.orElseThrow(EscalationPolicyNotFoundException::new);
		ManagedServiceView service = findManagedService
				.execute(command.managedServiceId())
				.orElseThrow(PolicyManagedServiceNotFoundException::new);
		EscalationPolicy updated = existing.update(
				new PolicyManagedService(service.id(), service.name()),
				command.priority(),
				command.acknowledgementDeadline(),
				command.resolutionDeadline());
		policies.findByManagedServiceIdAndPriority(
						service.id(),
						updated.priority())
				.filter(policy -> !Objects.equals(policy.id(), existing.id()))
				.ifPresent(policy -> {
					throw new DuplicateEscalationPolicyException();
				});
		return EscalationPolicyView.from(policies.save(updated));
	}
}
