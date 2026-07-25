package com.golubovicluka.incident_ops.identity.application.dto;

import com.golubovicluka.incident_ops.identity.domain.UserAccount;

public record AssignableUserView(
		Long id,
		String username,
		String displayName,
		TeamView team) {

	public static AssignableUserView from(UserAccount account) {
		return new AssignableUserView(
				account.id(),
				account.username(),
				account.displayName(),
				new TeamView(account.team().id(), account.team().name()));
	}

	public record TeamView(Long id, String name) {
	}
}
