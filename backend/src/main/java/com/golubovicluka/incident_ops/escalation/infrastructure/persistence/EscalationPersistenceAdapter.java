package com.golubovicluka.incident_ops.escalation.infrastructure.persistence;

import com.golubovicluka.incident_ops.escalation.domain.Escalation;
import com.golubovicluka.incident_ops.escalation.domain.EscalationActor;
import com.golubovicluka.incident_ops.escalation.domain.EscalationRepository;
import com.golubovicluka.incident_ops.identity.infrastructure.persistence.UserAccountJpaEntity;
import com.golubovicluka.incident_ops.incident.infrastructure.persistence.IncidentJpaEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class EscalationPersistenceAdapter implements EscalationRepository {

	private final SpringDataEscalationRepository repository;
	private final EntityManager entityManager;

	public EscalationPersistenceAdapter(
			SpringDataEscalationRepository repository,
			EntityManager entityManager) {
		this.repository = repository;
		this.entityManager = entityManager;
	}

	@Override
	public Escalation save(Escalation escalation) {
		EscalationJpaEntity saved = repository.saveAndFlush(
				new EscalationJpaEntity(
						entityManager.getReference(
								IncidentJpaEntity.class,
								escalation.incidentId()),
						escalation.level(),
						escalation.reason(),
						entityManager.getReference(
								UserAccountJpaEntity.class,
								escalation.actor().id()),
						escalation.escalatedAt()));
		return new Escalation(
				saved.getId(),
				saved.getIncident().getId(),
				saved.getLevel(),
				saved.getReason(),
				new EscalationActor(
						saved.getActor().getId(),
						saved.getActor().getUsername(),
						saved.getActor().getDisplayName()),
				saved.getEscalatedAt());
	}

	@Override
	public int findHighestLevel(long incidentId) {
		return repository.findHighestLevelByIncidentId(incidentId);
	}
}
