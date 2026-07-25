package com.golubovicluka.incident_ops.servicecatalog.infrastructure.persistence;

import com.golubovicluka.incident_ops.identity.infrastructure.persistence.TeamJpaEntity;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.OwningTeam;

final class ManagedServicePersistenceMapper {

	ManagedServiceJpaEntity toJpaEntity(
			ManagedService service,
			TeamJpaEntity owningTeam) {
		return new ManagedServiceJpaEntity(
				service.id(),
				service.name(),
				service.description(),
				service.criticality(),
				owningTeam);
	}

	ManagedService toDomain(ManagedServiceJpaEntity entity) {
		TeamJpaEntity team = entity.getOwningTeam();
		return new ManagedService(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getCriticality(),
				new OwningTeam(team.getId(), team.getName()));
	}
}
