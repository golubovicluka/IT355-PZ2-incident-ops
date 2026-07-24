package com.golubovicluka.incident_ops.identity.infrastructure.persistence;

import com.golubovicluka.incident_ops.identity.domain.Team;

final class TeamPersistenceMapper {

	TeamJpaEntity toJpaEntity(Team team) {
		return new TeamJpaEntity(team.id(), team.name());
	}

	Team toDomain(TeamJpaEntity entity) {
		return new Team(entity.getId(), entity.getName());
	}
}
