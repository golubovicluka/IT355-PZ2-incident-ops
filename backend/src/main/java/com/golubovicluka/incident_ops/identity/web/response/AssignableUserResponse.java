package com.golubovicluka.incident_ops.identity.web.response;

import com.golubovicluka.incident_ops.identity.application.dto.AssignableUserView;

public record AssignableUserResponse(
		Long id,
		String username,
		String displayName,
		TeamResponse team) {

	public static AssignableUserResponse from(AssignableUserView user) {
		return new AssignableUserResponse(
				user.id(),
				user.username(),
				user.displayName(),
				new TeamResponse(user.team().id(), user.team().name()));
	}

	public record TeamResponse(Long id, String name) {
	}
}
