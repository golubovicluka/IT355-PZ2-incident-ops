package com.golubovicluka.incident_ops.escalation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import com.golubovicluka.incident_ops.escalation.domain.Escalation;
import com.golubovicluka.incident_ops.escalation.domain.EscalationActor;
import com.golubovicluka.incident_ops.escalation.domain.EscalationRepository;
import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentManagedService;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;
import com.golubovicluka.incident_ops.integration.PostgreSQLContainerSupport;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import com.golubovicluka.incident_ops.servicecatalog.domain.OwningTeam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EscalationPersistenceAdapterTest extends PostgreSQLContainerSupport {

	private static final Instant CREATED_AT =
			Instant.parse("2026-07-25T08:15:30Z");

	@Autowired
	private TeamRepository teams;

	@Autowired
	private UserAccountRepository users;

	@Autowired
	private ManagedServiceRepository services;

	@Autowired
	private IncidentRepository incidents;

	@Autowired
	private EscalationRepository escalations;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private UserAccount actor;
	private Incident incident;

	@BeforeEach
	void setUpIncident() {
		long suffix = System.nanoTime();
		Team team = teams.save(Team.create("Escalation Team " + suffix));
		actor = users.save(UserAccount.create(
				"escalation-actor-" + suffix,
				"Escalation Actor",
				"$2a$10$escalationHash",
				Set.of(Role.RESPONDER),
				team));
		ManagedService service = services.save(ManagedService.create(
				"Escalation Service " + suffix,
				"Service used for escalation persistence.",
				Criticality.CRITICAL,
				new OwningTeam(team.id(), team.name())));
		IncidentUser incidentActor = new IncidentUser(
				actor.id(),
				actor.username(),
				actor.displayName());
		incident = incidents.save(Incident.create(
				"INC-ESCALATION-" + suffix,
				"Checkout unavailable",
				"All checkout attempts fail.",
				IncidentPriority.SEV1,
				new IncidentManagedService(service.id(), service.name()),
				incidentActor,
				null,
				CREATED_AT));
	}

	@Test
	void persistsIncidentActorAndIncreasingLevels() {
		Escalation first = escalations.save(escalation(1));
		Escalation second = escalations.save(escalation(2));

		assertThat(first.id()).isNotNull();
		assertThat(first.incidentId()).isEqualTo(incident.id());
		assertThat(first.actor().username()).isEqualTo(actor.username());
		assertThat(second.level()).isEqualTo(2);
		assertThat(escalations.findHighestLevel(incident.id())).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from escalations where incident_id = ?",
				Long.class,
				incident.id())).isEqualTo(2L);
	}

	private Escalation escalation(int level) {
		return Escalation.create(
				incident.id(),
				level,
				"Escalation reason " + level,
				new EscalationActor(
						actor.id(),
						actor.username(),
						actor.displayName()),
				CREATED_AT.plusSeconds(level * 60L));
	}
}
