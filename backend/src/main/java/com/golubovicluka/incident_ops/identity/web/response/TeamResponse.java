package com.golubovicluka.incident_ops.identity.web.response;

import com.golubovicluka.incident_ops.identity.application.dto.TeamView;

public record TeamResponse(Long id, String name) {

	public static TeamResponse from(TeamView team) {
		return new TeamResponse(team.id(), team.name());
	}
}
