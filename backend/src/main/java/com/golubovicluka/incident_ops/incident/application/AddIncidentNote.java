package com.golubovicluka.incident_ops.incident.application;

import java.time.Clock;
import java.time.Instant;

import com.golubovicluka.incident_ops.identity.application.FindAssignableUser;
import com.golubovicluka.incident_ops.identity.application.dto.AssignableUserView;
import com.golubovicluka.incident_ops.incident.application.command.AddIncidentNoteCommand;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentNotFoundException;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddIncidentNote {

	private final IncidentRepository incidents;
	private final FindAssignableUser findAssignableUser;
	private final Clock clock;

	public AddIncidentNote(
			IncidentRepository incidents,
			FindAssignableUser findAssignableUser,
			Clock clock) {
		this.incidents = incidents;
		this.findAssignableUser = findAssignableUser;
		this.clock = clock;
	}

	@Transactional
	public IncidentDetailView execute(AddIncidentNoteCommand command) {
		Incident incident = incidents.findById(command.id())
				.orElseThrow(IncidentNotFoundException::new);
		IncidentUser actor = findAssignableUser
				.byUsername(command.actorUsername())
				.map(AddIncidentNote::toIncidentUser)
				.orElseThrow(IncidentActorNotFoundException::new);
		Incident noted = incident.addNote(
				command.note(),
				actor,
				Instant.now(clock));
		return IncidentDetailView.from(incidents.save(noted));
	}

	private static IncidentUser toIncidentUser(AssignableUserView user) {
		return new IncidentUser(user.id(), user.username(), user.displayName());
	}
}
