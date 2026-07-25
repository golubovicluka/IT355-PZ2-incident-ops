package com.golubovicluka.incident_ops.escalation.infrastructure.persistence;

import java.time.Duration;

import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicy;
import com.golubovicluka.incident_ops.escalation.domain.PolicyManagedService;
import com.golubovicluka.incident_ops.servicecatalog.infrastructure.persistence.ManagedServiceJpaEntity;

final class EscalationPolicyPersistenceMapper {

	EscalationPolicyJpaEntity toJpaEntity(
			EscalationPolicy policy,
			ManagedServiceJpaEntity managedService) {
		return new EscalationPolicyJpaEntity(
				policy.id(),
				managedService,
				policy.priority(),
				policy.acknowledgementDeadline().toMinutes(),
				policy.resolutionDeadline().toMinutes());
	}

	EscalationPolicy toDomain(EscalationPolicyJpaEntity entity) {
		ManagedServiceJpaEntity service = entity.getManagedService();
		return new EscalationPolicy(
				entity.getId(),
				new PolicyManagedService(service.getId(), service.getName()),
				entity.getPriority(),
				Duration.ofMinutes(entity.getAcknowledgementMinutes()),
				Duration.ofMinutes(entity.getResolutionMinutes()));
	}
}
