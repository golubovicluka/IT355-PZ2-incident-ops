package com.golubovicluka.incident_ops.identity.application;

import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamInUseException;
import com.golubovicluka.incident_ops.identity.domain.TeamNotFoundException;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteTeam {

	private final TeamRepository teams;

	public DeleteTeam(TeamRepository teams) {
		this.teams = teams;
	}

	@Transactional
	public void execute(long id) {
		Team team = teams.findById(id).orElseThrow(TeamNotFoundException::new);
		if (teams.isReferencedByUserAccount(id)) {
			throw new TeamInUseException();
		}
		teams.delete(team);
	}
}
