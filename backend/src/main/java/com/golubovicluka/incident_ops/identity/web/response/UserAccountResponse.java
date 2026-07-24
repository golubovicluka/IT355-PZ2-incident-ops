package com.golubovicluka.incident_ops.identity.web.response;

import java.util.Set;

import com.golubovicluka.incident_ops.identity.application.UserAccountView;

public record UserAccountResponse(
		Long id,
		String username,
		String displayName,
		Set<String> roles,
		TeamResponse team) {

	public UserAccountResponse {
		roles = Set.copyOf(roles);
	}

	public static UserAccountResponse from(UserAccountView view) {
		return new UserAccountResponse(
				view.id(),
				view.username(),
				view.displayName(),
				view.roles(),
				new TeamResponse(view.team().id(), view.team().name()));
	}

	public record TeamResponse(Long id, String name) {
	}
}
