package com.golubovicluka.incident_ops.incident.application;

import java.time.Clock;
import java.time.Instant;

import com.golubovicluka.incident_ops.identity.application.FindAssignableUser;
import com.golubovicluka.incident_ops.identity.application.dto.AssignableUserView;
import com.golubovicluka.incident_ops.incident.application.command.UpdateIncidentCommand;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentManagedService;
import com.golubovicluka.incident_ops.incident.domain.IncidentNotFoundException;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;
import com.golubovicluka.incident_ops.servicecatalog.application.FindManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateIncident {

	private final IncidentRepository incidents;
	private final FindManagedService findManagedService;
	private final FindAssignableUser findAssignableUser;
	private final Clock clock;

	public UpdateIncident(
			IncidentRepository incidents,
			FindManagedService findManagedService,
			FindAssignableUser findAssignableUser,
			Clock clock) {
		this.incidents = incidents;
		this.findManagedService = findManagedService;
		this.findAssignableUser = findAssignableUser;
		this.clock = clock;
	}

	@Transactional
	public IncidentDetailView execute(UpdateIncidentCommand command) {
		Incident existing = incidents.findById(command.id())
				.orElseThrow(IncidentNotFoundException::new);
		ManagedServiceView service = findManagedService
				.execute(command.managedServiceId())
				.orElseThrow(IncidentManagedServiceNotFoundException::new);
		IncidentUser assignee = command.assigneeId() == null
				? null
				: findAssignableUser.byId(command.assigneeId())
						.map(UpdateIncident::toIncidentUser)
						.orElseThrow(IncidentAssigneeNotFoundException::new);
		Incident updated = existing.update(
				command.title(),
				command.description(),
				command.priority(),
				new IncidentManagedService(service.id(), service.name()),
				assignee,
				Instant.now(clock));
		return IncidentDetailView.from(incidents.save(updated));
	}

	private static IncidentUser toIncidentUser(AssignableUserView user) {
		return new IncidentUser(user.id(), user.username(), user.displayName());
	}
}
