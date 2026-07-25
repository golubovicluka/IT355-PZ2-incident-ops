package com.golubovicluka.incident_ops.incident.application;

import java.time.Clock;
import java.time.Instant;

import com.golubovicluka.incident_ops.identity.application.FindAssignableUser;
import com.golubovicluka.incident_ops.identity.application.dto.AssignableUserView;
import com.golubovicluka.incident_ops.incident.application.command.CreateIncidentCommand;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentManagedService;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;
import com.golubovicluka.incident_ops.servicecatalog.application.FindManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateIncident {

	private final IncidentRepository incidents;
	private final FindManagedService findManagedService;
	private final FindAssignableUser findAssignableUser;
	private final ReferenceCodeGenerator referenceCodeGenerator;
	private final Clock clock;

	public CreateIncident(
			IncidentRepository incidents,
			FindManagedService findManagedService,
			FindAssignableUser findAssignableUser,
			ReferenceCodeGenerator referenceCodeGenerator,
			Clock clock) {
		this.incidents = incidents;
		this.findManagedService = findManagedService;
		this.findAssignableUser = findAssignableUser;
		this.referenceCodeGenerator = referenceCodeGenerator;
		this.clock = clock;
	}

	@Transactional
	public IncidentDetailView execute(CreateIncidentCommand command) {
		ManagedServiceView service = findManagedService
				.execute(command.managedServiceId())
				.orElseThrow(IncidentManagedServiceNotFoundException::new);
		AssignableUserView reporter = findAssignableUser
				.byUsername(command.reporterUsername())
				.orElseThrow(IncidentReporterNotFoundException::new);
		IncidentUser assignee = command.assigneeId() == null
				? null
				: findAssignableUser.byId(command.assigneeId())
						.map(CreateIncident::toIncidentUser)
						.orElseThrow(IncidentAssigneeNotFoundException::new);
		Instant now = Instant.now(clock);
		Incident incident = Incident.create(
				referenceCodeGenerator.nextReferenceCode(),
				command.title(),
				command.description(),
				command.priority(),
				new IncidentManagedService(service.id(), service.name()),
				toIncidentUser(reporter),
				assignee,
				now);
		return IncidentDetailView.from(incidents.save(incident));
	}

	private static IncidentUser toIncidentUser(AssignableUserView user) {
		return new IncidentUser(user.id(), user.username(), user.displayName());
	}
}
