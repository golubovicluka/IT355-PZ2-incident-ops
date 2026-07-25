package com.golubovicluka.incident_ops.incident.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.identity.infrastructure.persistence.UserAccountJpaEntity;
import com.golubovicluka.incident_ops.incident.domain.DuplicateIncidentReferenceCodeException;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentCriteria;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import com.golubovicluka.incident_ops.servicecatalog.infrastructure.persistence.ManagedServiceJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class IncidentPersistenceAdapter implements IncidentRepository {

	private static final Sort NEWEST_FIRST = Sort.by(
			Sort.Order.desc("createdAt"),
			Sort.Order.desc("id"));

	private final SpringDataIncidentRepository repository;
	private final EntityManager entityManager;
	private final IncidentPersistenceMapper mapper =
			new IncidentPersistenceMapper();

	public IncidentPersistenceAdapter(
			SpringDataIncidentRepository repository,
			EntityManager entityManager) {
		this.repository = repository;
		this.entityManager = entityManager;
	}

	@Override
	public Incident save(Incident incident) {
		try {
			if (incident.id() == null) {
				return saveNew(incident);
			}
			return updateExisting(incident);
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateIncidentReferenceCodeException(exception);
		}
	}

	@Override
	public List<Incident> findAll(IncidentCriteria criteria) {
		return repository.findAll(specification(criteria), NEWEST_FIRST).stream()
				.map(mapper::toDomain)
				.toList();
	}

	@Override
	public Optional<Incident> findById(long id) {
		return repository.findById(id).map(mapper::toDomain);
	}

	private Incident saveNew(Incident incident) {
		ManagedServiceJpaEntity service = serviceReference(
				incident.managedService().id());
		UserAccountJpaEntity reporter = userReference(incident.reporter().id());
		UserAccountJpaEntity assignee = incident.assignee() == null
				? null
				: userReference(incident.assignee().id());
		IncidentJpaEntity saved = repository.saveAndFlush(
				mapper.toNewJpaEntity(
						incident,
						service,
						reporter,
						assignee,
						this::userReference));
		return mapper.toDomain(saved);
	}

	private Incident updateExisting(Incident incident) {
		IncidentJpaEntity entity = repository.findById(incident.id())
				.orElseThrow(() -> new IllegalStateException(
						"Incident disappeared during update"));
		entity.updateEditableFields(
				incident.title(),
				incident.description(),
				incident.priority(),
				incident.status(),
				serviceReference(incident.managedService().id()),
				incident.assignee() == null
						? null
						: userReference(incident.assignee().id()),
				incident.updatedAt(),
				incident.acknowledgedAt(),
				incident.resolvedAt());
		incident.events().stream()
				.filter(event -> event.id() == null)
				.forEach(event -> entity.addEvent(new IncidentEventJpaEntity(
						entity,
						event.kind(),
						userReference(event.actor().id()),
						event.previousStatus(),
						event.newStatus(),
						event.occurredAt())));
		repository.flush();
		return mapper.toDomain(entity);
	}

	private Specification<IncidentJpaEntity> specification(
			IncidentCriteria criteria) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (criteria.status() != null) {
				predicates.add(builder.equal(
						root.get("status"),
						criteria.status()));
			}
			if (criteria.priority() != null) {
				predicates.add(builder.equal(
						root.get("priority"),
						criteria.priority()));
			}
			if (criteria.managedServiceId() != null) {
				predicates.add(builder.equal(
						root.get("managedService").get("id"),
						criteria.managedServiceId()));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private ManagedServiceJpaEntity serviceReference(long id) {
		return entityManager.getReference(ManagedServiceJpaEntity.class, id);
	}

	private UserAccountJpaEntity userReference(long id) {
		return entityManager.getReference(UserAccountJpaEntity.class, id);
	}
}
