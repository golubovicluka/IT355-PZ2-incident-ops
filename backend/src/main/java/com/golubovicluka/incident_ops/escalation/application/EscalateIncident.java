package com.golubovicluka.incident_ops.escalation.application;

import java.time.Clock;
import java.time.Instant;

import com.golubovicluka.incident_ops.escalation.application.command.EscalateIncidentCommand;
import com.golubovicluka.incident_ops.escalation.domain.Escalation;
import com.golubovicluka.incident_ops.escalation.domain.EscalationActor;
import com.golubovicluka.incident_ops.escalation.domain.EscalationRepository;
import com.golubovicluka.incident_ops.identity.application.FindAssignableUser;
import com.golubovicluka.incident_ops.identity.application.dto.AssignableUserView;
import com.golubovicluka.incident_ops.incident.application.IncidentActorNotFoundException;
import com.golubovicluka.incident_ops.incident.application.RecordIncidentEscalation;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EscalateIncident {

	private final EscalationRepository escalations;
	private final FindAssignableUser findAssignableUser;
	private final RecordIncidentEscalation recordIncidentEscalation;
	private final Clock clock;

	public EscalateIncident(
			EscalationRepository escalations,
			FindAssignableUser findAssignableUser,
			RecordIncidentEscalation recordIncidentEscalation,
			Clock clock) {
		this.escalations = escalations;
		this.findAssignableUser = findAssignableUser;
		this.recordIncidentEscalation = recordIncidentEscalation;
		this.clock = clock;
	}

	@Transactional
	public IncidentDetailView execute(EscalateIncidentCommand command) {
		AssignableUserView user = findAssignableUser
				.byUsername(command.actorUsername())
				.orElseThrow(IncidentActorNotFoundException::new);
		int level = Escalation.nextLevel(
				escalations.findHighestLevel(command.incidentId()));
		Instant now = Instant.now(clock);
		Escalation escalation = Escalation.create(
				command.incidentId(),
				level,
				command.reason(),
				toEscalationActor(user),
				now);
		IncidentDetailView incident = recordIncidentEscalation.execute(
				command.incidentId(),
				level,
				escalation.reason(),
				toIncidentUser(user),
				now);
		escalations.save(escalation);
		return incident;
	}

	private static EscalationActor toEscalationActor(AssignableUserView user) {
		return new EscalationActor(
				user.id(),
				user.username(),
				user.displayName());
	}

	private static IncidentUser toIncidentUser(AssignableUserView user) {
		return new IncidentUser(
				user.id(),
				user.username(),
				user.displayName());
	}
}
