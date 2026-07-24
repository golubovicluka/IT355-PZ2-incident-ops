package com.golubovicluka.incident_ops.identity.web;

import java.util.List;

import com.golubovicluka.incident_ops.identity.application.CreateTeam;
import com.golubovicluka.incident_ops.identity.application.DeleteTeam;
import com.golubovicluka.incident_ops.identity.application.ListTeams;
import com.golubovicluka.incident_ops.identity.application.UpdateTeam;
import com.golubovicluka.incident_ops.identity.application.command.CreateTeamCommand;
import com.golubovicluka.incident_ops.identity.application.command.UpdateTeamCommand;
import com.golubovicluka.incident_ops.identity.application.dto.TeamView;
import com.golubovicluka.incident_ops.identity.web.request.TeamRequest;
import com.golubovicluka.incident_ops.identity.web.response.TeamResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/admin/teams")
public class TeamController {

	private final ListTeams listTeams;
	private final CreateTeam createTeam;
	private final UpdateTeam updateTeam;
	private final DeleteTeam deleteTeam;

	public TeamController(
			ListTeams listTeams,
			CreateTeam createTeam,
			UpdateTeam updateTeam,
			DeleteTeam deleteTeam) {
		this.listTeams = listTeams;
		this.createTeam = createTeam;
		this.updateTeam = updateTeam;
		this.deleteTeam = deleteTeam;
	}

	@GetMapping
	List<TeamResponse> list() {
		return listTeams.execute().stream().map(TeamResponse::from).toList();
	}

	@PostMapping
	ResponseEntity<TeamResponse> create(@Valid @RequestBody TeamRequest request) {
		TeamView created = createTeam.execute(new CreateTeamCommand(request.name()));
		return ResponseEntity
				.created(ServletUriComponentsBuilder.fromCurrentRequest()
						.path("/{id}")
						.buildAndExpand(created.id())
						.toUri())
				.body(TeamResponse.from(created));
	}

	@PutMapping("/{id}")
	TeamResponse update(
			@PathVariable long id,
			@Valid @RequestBody TeamRequest request) {
		return TeamResponse.from(updateTeam.execute(
				new UpdateTeamCommand(id, request.name())));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable long id) {
		deleteTeam.execute(id);
		return ResponseEntity.noContent().build();
	}
}
