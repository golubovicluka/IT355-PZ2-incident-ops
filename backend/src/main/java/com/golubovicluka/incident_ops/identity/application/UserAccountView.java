package com.golubovicluka.incident_ops.identity.application;

import java.util.Set;
import java.util.stream.Collectors;

import com.golubovicluka.incident_ops.identity.domain.UserAccount;

public record UserAccountView(
		Long id,
		String username,
		String displayName,
		Set<String> roles,
		TeamView team) {

	public UserAccountView {
		roles = Set.copyOf(roles);
	}

	public static UserAccountView from(UserAccount account) {
		return new UserAccountView(
				account.id(),
				account.username(),
				account.displayName(),
				account.roles().stream()
						.map(Enum::name)
						.collect(Collectors.toUnmodifiableSet()),
				new TeamView(account.team().id(), account.team().name()));
	}

	public record TeamView(Long id, String name) {
	}
}
