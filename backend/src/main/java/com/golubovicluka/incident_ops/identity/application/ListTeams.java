package com.golubovicluka.incident_ops.identity.application;

import java.util.List;

import com.golubovicluka.incident_ops.identity.application.dto.TeamView;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListTeams {

	private final TeamRepository teams;

	public ListTeams(TeamRepository teams) {
		this.teams = teams;
	}

	@Transactional(readOnly = true)
	public List<TeamView> execute() {
		return teams.findAll().stream().map(TeamView::from).toList();
	}
}
