package com.golubovicluka.incident_ops.escalation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.escalation.domain.DuplicateEscalationPolicyException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicy;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyInUseException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyRepository;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.servicecatalog.infrastructure.persistence.ManagedServiceJpaEntity;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class EscalationPolicyPersistenceAdapter
		implements EscalationPolicyRepository {

	private final SpringDataEscalationPolicyRepository repository;
	private final EntityManager entityManager;
	private final EscalationPolicyPersistenceMapper mapper =
			new EscalationPolicyPersistenceMapper();

	public EscalationPolicyPersistenceAdapter(
			SpringDataEscalationPolicyRepository repository,
			EntityManager entityManager) {
		this.repository = repository;
		this.entityManager = entityManager;
	}

	@Override
	public EscalationPolicy save(EscalationPolicy policy) {
		ManagedServiceJpaEntity service = entityManager.getReference(
				ManagedServiceJpaEntity.class,
				policy.managedService().id());
		try {
			EscalationPolicyJpaEntity saved = repository.saveAndFlush(
					mapper.toJpaEntity(policy, service));
			return new EscalationPolicy(
					saved.getId(),
					policy.managedService(),
					policy.priority(),
					policy.acknowledgementDeadline(),
					policy.resolutionDeadline());
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateEscalationPolicyException(exception);
		}
	}

	@Override
	public List<EscalationPolicy> findAll() {
		return repository.findAllByOrderByManagedServiceNameAscPriorityAsc()
				.stream()
				.map(mapper::toDomain)
				.toList();
	}

	@Override
	public Optional<EscalationPolicy> findById(long id) {
		return repository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<EscalationPolicy> findByManagedServiceIdAndPriority(
			long managedServiceId,
			IncidentPriority priority) {
		return repository.findByManagedServiceIdAndPriority(
				managedServiceId,
				priority).map(mapper::toDomain);
	}

	@Override
	public void delete(EscalationPolicy policy) {
		try {
			repository.deleteById(policy.id());
			// Flush inside this boundary so foreign-key protected rule data
			// becomes a stable conflict instead of a late transaction failure.
			repository.flush();
		} catch (DataIntegrityViolationException exception) {
			throw new EscalationPolicyInUseException(exception);
		}
	}
}
