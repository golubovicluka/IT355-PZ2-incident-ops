package com.golubovicluka.incident_ops.identity.application;

import java.util.Optional;

import com.golubovicluka.incident_ops.identity.application.dto.TeamView;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindTeam {

	private final TeamRepository teams;

	public FindTeam(TeamRepository teams) {
		this.teams = teams;
	}

	@Transactional(readOnly = true)
	public Optional<TeamView> execute(long id) {
		return teams.findById(id).map(TeamView::from);
	}
}
