package com.golubovicluka.incident_ops.servicecatalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamInUseException;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import com.golubovicluka.incident_ops.integration.PostgreSQLContainerSupport;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import com.golubovicluka.incident_ops.servicecatalog.domain.DuplicateManagedServiceNameException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import com.golubovicluka.incident_ops.servicecatalog.domain.OwningTeam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ManagedServicePersistenceAdapterTest extends PostgreSQLContainerSupport {

	@Autowired
	private TeamRepository teams;

	@Autowired
	private ManagedServiceRepository services;

	@Test
	void persistsServiceTeamRelationAndListsByName() {
		Team team = teams.save(Team.create("Platform Operations"));
		ManagedService payments = services.save(ManagedService.create(
				"Payments API",
				"Processes card payments.",
				Criticality.CRITICAL,
				new OwningTeam(team.id(), team.name())));
		services.save(ManagedService.create(
				"Checkout API",
				"Coordinates checkout.",
				Criticality.HIGH,
				new OwningTeam(team.id(), team.name())));

		assertThat(services.findById(payments.id())).contains(payments);
		assertThat(services.findAll())
				.extracting(ManagedService::name)
				.containsExactly("Checkout API", "Payments API");

		assertThatThrownBy(() -> teams.delete(team))
				.isInstanceOf(TeamInUseException.class);
	}

	@Test
	void databaseConstraintRejectsDuplicateServiceName() {
		Team team = teams.save(Team.create("Service Owners"));
		ManagedService service = ManagedService.create(
				"Customer API",
				"Provides customer data.",
				Criticality.MEDIUM,
				new OwningTeam(team.id(), team.name()));
		services.save(service);

		assertThatThrownBy(() -> services.save(service))
				.isInstanceOf(DuplicateManagedServiceNameException.class);
	}
}
