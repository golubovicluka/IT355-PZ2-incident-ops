package com.golubovicluka.incident_ops.servicecatalog.application;

import java.util.Objects;

import com.golubovicluka.incident_ops.identity.application.FindTeam;
import com.golubovicluka.incident_ops.identity.application.dto.TeamView;
import com.golubovicluka.incident_ops.servicecatalog.application.command.UpdateManagedServiceCommand;
import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import com.golubovicluka.incident_ops.servicecatalog.domain.DuplicateManagedServiceNameException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceNotFoundException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import com.golubovicluka.incident_ops.servicecatalog.domain.OwningTeam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateManagedService {

	private final ManagedServiceRepository services;
	private final FindTeam findTeam;

	public UpdateManagedService(ManagedServiceRepository services, FindTeam findTeam) {
		this.services = services;
		this.findTeam = findTeam;
	}

	@Transactional
	public ManagedServiceView execute(UpdateManagedServiceCommand command) {
		ManagedService existing = services.findById(command.id())
				.orElseThrow(ManagedServiceNotFoundException::new);
		TeamView team = findTeam.execute(command.owningTeamId())
				.orElseThrow(OwningTeamNotFoundException::new);
		ManagedService updated = existing.update(
				command.name(),
				command.description(),
				command.criticality(),
				new OwningTeam(team.id(), team.name()));
		services.findByName(updated.name())
				.filter(service -> !Objects.equals(service.id(), existing.id()))
				.ifPresent(service -> {
					throw new DuplicateManagedServiceNameException();
				});
		return ManagedServiceView.from(services.save(updated));
	}
}
