package com.golubovicluka.incident_ops.incident.infrastructure.persistence;

import java.util.function.Function;

import com.golubovicluka.incident_ops.identity.infrastructure.persistence.UserAccountJpaEntity;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentEvent;
import com.golubovicluka.incident_ops.incident.domain.IncidentManagedService;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;
import com.golubovicluka.incident_ops.servicecatalog.infrastructure.persistence.ManagedServiceJpaEntity;

final class IncidentPersistenceMapper {

	IncidentJpaEntity toNewJpaEntity(
			Incident incident,
			ManagedServiceJpaEntity managedService,
			UserAccountJpaEntity reporter,
			UserAccountJpaEntity assignee,
			Function<Long, UserAccountJpaEntity> userReference) {
		IncidentJpaEntity entity = new IncidentJpaEntity(
				incident.referenceCode(),
				incident.title(),
				incident.description(),
				incident.priority(),
				incident.status(),
				managedService,
				reporter,
				assignee,
				incident.createdAt(),
				incident.updatedAt(),
				incident.acknowledgedAt(),
				incident.resolvedAt());
		incident.events().forEach(event -> entity.addEvent(
				new IncidentEventJpaEntity(
						entity,
						event.kind(),
						userReference.apply(event.actor().id()),
						event.previousStatus(),
						event.newStatus(),
						event.note(),
						event.occurredAt())));
		return entity;
	}

	Incident toDomain(IncidentJpaEntity entity) {
		ManagedServiceJpaEntity service = entity.getManagedService();
		return new Incident(
				entity.getId(),
				entity.getReferenceCode(),
				entity.getTitle(),
				entity.getDescription(),
				entity.getPriority(),
				entity.getStatus(),
				new IncidentManagedService(service.getId(), service.getName()),
				toDomain(entity.getReporter()),
				toNullableDomain(entity.getAssignee()),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getAcknowledgedAt(),
				entity.getResolvedAt(),
				entity.getEvents().stream().map(this::toDomain).toList());
	}

	private IncidentEvent toDomain(IncidentEventJpaEntity entity) {
		return new IncidentEvent(
				entity.getId(),
				entity.getKind(),
				toDomain(entity.getActor()),
				entity.getPreviousStatus(),
				entity.getNewStatus(),
				entity.getNote(),
				entity.getOccurredAt());
	}

	private IncidentUser toNullableDomain(UserAccountJpaEntity entity) {
		return entity == null ? null : toDomain(entity);
	}

	private IncidentUser toDomain(UserAccountJpaEntity entity) {
		return new IncidentUser(
				entity.getId(),
				entity.getUsername(),
				entity.getDisplayName());
	}
}
