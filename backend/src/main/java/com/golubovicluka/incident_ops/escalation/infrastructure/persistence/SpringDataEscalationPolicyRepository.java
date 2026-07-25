package com.golubovicluka.incident_ops.escalation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataEscalationPolicyRepository
		extends JpaRepository<EscalationPolicyJpaEntity, Long> {

	@EntityGraph(attributePaths = "managedService")
	List<EscalationPolicyJpaEntity>
			findAllByOrderByManagedServiceNameAscPriorityAsc();

	@Override
	@EntityGraph(attributePaths = "managedService")
	Optional<EscalationPolicyJpaEntity> findById(Long id);

	@EntityGraph(attributePaths = "managedService")
	Optional<EscalationPolicyJpaEntity> findByManagedServiceIdAndPriority(
			long managedServiceId,
			IncidentPriority priority);
}
