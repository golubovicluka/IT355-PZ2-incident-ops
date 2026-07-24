package com.golubovicluka.incident_ops.identity.application.dto;

import com.golubovicluka.incident_ops.identity.domain.Team;

public record TeamView(Long id, String name) {

	public static TeamView from(Team team) {
		return new TeamView(team.id(), team.name());
	}
}
