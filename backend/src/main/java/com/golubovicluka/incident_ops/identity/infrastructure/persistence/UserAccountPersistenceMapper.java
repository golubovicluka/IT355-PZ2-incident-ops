package com.golubovicluka.incident_ops.identity.infrastructure.persistence;

import java.util.Set;
import java.util.stream.Collectors;

import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;

final class UserAccountPersistenceMapper {

	UserAccountJpaEntity toJpaEntity(UserAccount account, TeamJpaEntity team) {
		return new UserAccountJpaEntity(
				account.id(),
				account.username(),
				account.displayName(),
				account.passwordHash(),
				toJpaRoles(account.roles()),
				team);
	}

	UserAccount toDomain(UserAccountJpaEntity entity) {
		TeamJpaEntity team = entity.getTeam();
		return new UserAccount(
				entity.getId(),
				entity.getUsername(),
				entity.getDisplayName(),
				entity.getPasswordHash(),
				toDomainRoles(entity.getRoles()),
				new Team(team.getId(), team.getName()));
	}

	private Set<UserRoleJpa> toJpaRoles(Set<Role> roles) {
		return roles.stream()
				.map(role -> UserRoleJpa.valueOf(role.name()))
				.collect(Collectors.toUnmodifiableSet());
	}

	private Set<Role> toDomainRoles(Set<UserRoleJpa> roles) {
		return roles.stream()
				.map(role -> Role.valueOf(role.name()))
				.collect(Collectors.toUnmodifiableSet());
	}
}
