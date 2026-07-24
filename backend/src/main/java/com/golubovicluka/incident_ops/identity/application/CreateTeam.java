package com.golubovicluka.incident_ops.identity.application;

import com.golubovicluka.incident_ops.identity.application.command.CreateTeamCommand;
import com.golubovicluka.incident_ops.identity.application.dto.TeamView;
import com.golubovicluka.incident_ops.identity.domain.DuplicateTeamNameException;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateTeam {

	private final TeamRepository teams;

	public CreateTeam(TeamRepository teams) {
		this.teams = teams;
	}

	@Transactional
	public TeamView execute(CreateTeamCommand command) {
		Team team = Team.create(command.name());
		if (teams.findByName(team.name()).isPresent()) {
			throw new DuplicateTeamNameException();
		}
		return TeamView.from(teams.save(team));
	}
}
