package com.golubovicluka.incident_ops.identity.application;

import java.util.Objects;

import com.golubovicluka.incident_ops.identity.application.command.UpdateTeamCommand;
import com.golubovicluka.incident_ops.identity.application.dto.TeamView;
import com.golubovicluka.incident_ops.identity.domain.DuplicateTeamNameException;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamNotFoundException;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateTeam {

	private final TeamRepository teams;

	public UpdateTeam(TeamRepository teams) {
		this.teams = teams;
	}

	@Transactional
	public TeamView execute(UpdateTeamCommand command) {
		Team existing = teams.findById(command.id())
				.orElseThrow(TeamNotFoundException::new);
		Team renamed = existing.rename(command.name());
		teams.findByName(renamed.name())
				.filter(team -> !Objects.equals(team.id(), existing.id()))
				.ifPresent(team -> {
					throw new DuplicateTeamNameException();
				});
		return TeamView.from(teams.save(renamed));
	}
}
