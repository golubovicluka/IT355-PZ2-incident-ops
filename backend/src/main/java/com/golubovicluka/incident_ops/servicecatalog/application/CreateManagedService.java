package com.golubovicluka.incident_ops.servicecatalog.application;

import com.golubovicluka.incident_ops.identity.application.FindTeam;
import com.golubovicluka.incident_ops.identity.application.dto.TeamView;
import com.golubovicluka.incident_ops.servicecatalog.application.command.CreateManagedServiceCommand;
import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import com.golubovicluka.incident_ops.servicecatalog.domain.DuplicateManagedServiceNameException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import com.golubovicluka.incident_ops.servicecatalog.domain.OwningTeam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateManagedService {

	private final ManagedServiceRepository services;
	private final FindTeam findTeam;

	public CreateManagedService(ManagedServiceRepository services, FindTeam findTeam) {
		this.services = services;
		this.findTeam = findTeam;
	}

	@Transactional
	public ManagedServiceView execute(CreateManagedServiceCommand command) {
		TeamView team = findTeam.execute(command.owningTeamId())
				.orElseThrow(OwningTeamNotFoundException::new);
		ManagedService service = ManagedService.create(
				command.name(),
				command.description(),
				command.criticality(),
				new OwningTeam(team.id(), team.name()));
		if (services.findByName(service.name()).isPresent()) {
			throw new DuplicateManagedServiceNameException();
		}
		return ManagedServiceView.from(services.save(service));
	}
}
