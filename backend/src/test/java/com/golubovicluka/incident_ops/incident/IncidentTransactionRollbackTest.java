package com.golubovicluka.incident_ops.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.golubovicluka.incident_ops.escalation.application.EscalateIncident;
import com.golubovicluka.incident_ops.escalation.application.command.EscalateIncidentCommand;
import com.golubovicluka.incident_ops.escalation.domain.Escalation;
import com.golubovicluka.incident_ops.escalation.domain.EscalationRepository;
import com.golubovicluka.incident_ops.escalation.infrastructure.persistence.EscalationPersistenceAdapter;
import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import com.golubovicluka.incident_ops.incident.application.CreateIncident;
import com.golubovicluka.incident_ops.incident.application.ReferenceCodeGenerator;
import com.golubovicluka.incident_ops.incident.application.UpdateIncident;
import com.golubovicluka.incident_ops.incident.application.command.CreateIncidentCommand;
import com.golubovicluka.incident_ops.incident.application.command.UpdateIncidentCommand;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentCriteria;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import com.golubovicluka.incident_ops.incident.infrastructure.persistence.IncidentPersistenceAdapter;
import com.golubovicluka.incident_ops.integration.PostgreSQLContainerSupport;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import com.golubovicluka.incident_ops.servicecatalog.domain.OwningTeam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@Import(IncidentTransactionRollbackTest.FaultInjectionConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IncidentTransactionRollbackTest extends PostgreSQLContainerSupport {

	@Autowired
	private TeamRepository teams;

	@Autowired
	private UserAccountRepository users;

	@Autowired
	private ManagedServiceRepository services;

	@Autowired
	private CreateIncident createIncident;

	@Autowired
	private UpdateIncident updateIncident;

	@Autowired
	private EscalateIncident escalateIncident;

	@Autowired
	private FailAfterSaveIncidentRepository incidents;

	@Autowired
	private FailAfterSaveEscalationRepository escalations;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private UserAccount reporter;
	private ManagedService managedService;

	@BeforeEach
	void setUpCatalogs() {
		long suffix = System.nanoTime();
		Team team = teams.save(Team.create("Rollback Team " + suffix));
		reporter = users.save(UserAccount.create(
				"rollback-reporter-" + suffix,
				"Rollback Reporter",
				"$2a$10$rollbackHash",
				Set.of(Role.RESPONDER),
				team));
		managedService = services.save(ManagedService.create(
				"Rollback Service " + suffix,
				"Service used by transaction tests.",
				Criticality.HIGH,
				new OwningTeam(team.id(), team.name())));
		incidents.allowSaves();
		escalations.allowSaves();
	}

	@Test
	void failedCreateRollsBackIncidentAndCreatedEvent() {
		long incidentCount = countRows("incidents");
		long eventCount = countRows("incident_events");
		incidents.failNextSave();

		assertThatThrownBy(() -> createIncident.execute(createCommand()))
				.isInstanceOf(SimulatedPersistenceFailure.class);

		assertThat(countRows("incidents")).isEqualTo(incidentCount);
		assertThat(countRows("incident_events")).isEqualTo(eventCount);
	}

	@Test
	void failedEditRollsBackIncidentChangesAndPreservesTimeline() {
		IncidentDetailView created = createIncident.execute(createCommand());
		long eventCount = countRows("incident_events");
		incidents.failNextSave();

		assertThatThrownBy(() -> updateIncident.execute(
				new UpdateIncidentCommand(
						created.id(),
						"Title that must roll back",
						"Description that must roll back.",
						IncidentPriority.SEV2,
						managedService.id(),
						null)))
				.isInstanceOf(SimulatedPersistenceFailure.class);

		assertThat(jdbcTemplate.queryForObject(
				"select title from incidents where id = ?",
				String.class,
				created.id())).isEqualTo("Rollback test incident");
		assertThat(countRows("incident_events")).isEqualTo(eventCount);
	}

	@Test
	void failedEscalationRollsBackEscalationAndTimelineEvent() {
		IncidentDetailView created = createIncident.execute(createCommand());
		long eventCount = countRows("incident_events");
		long escalationCount = countRows("escalations");
		escalations.failNextSave();

		assertThatThrownBy(() -> escalateIncident.execute(
				new EscalateIncidentCommand(
						created.id(),
						"Customer impact requires escalation.",
						reporter.username())))
				.isInstanceOf(SimulatedPersistenceFailure.class);

		assertThat(countRows("incident_events")).isEqualTo(eventCount);
		assertThat(countRows("escalations")).isEqualTo(escalationCount);
	}

	private CreateIncidentCommand createCommand() {
		return new CreateIncidentCommand(
				"Rollback test incident",
				"Tests transaction rollback after a persistence flush.",
				IncidentPriority.SEV1,
				managedService.id(),
				null,
				reporter.username());
	}

	private long countRows(String table) {
		return jdbcTemplate.queryForObject(
				"select count(*) from " + table,
				Long.class);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FaultInjectionConfiguration {

		@Bean
		@Primary
		FailAfterSaveIncidentRepository failAfterSaveIncidentRepository(
				IncidentPersistenceAdapter delegate) {
			return new FailAfterSaveIncidentRepository(delegate);
		}

		@Bean
		@Primary
		ReferenceCodeGenerator deterministicReferenceCodeGenerator() {
			AtomicInteger sequence = new AtomicInteger();
			return () -> "INC-ROLLBACK-%04d".formatted(
					sequence.incrementAndGet());
		}

		@Bean
		@Primary
		FailAfterSaveEscalationRepository failAfterSaveEscalationRepository(
				EscalationPersistenceAdapter delegate) {
			return new FailAfterSaveEscalationRepository(delegate);
		}

		@Bean
		@Primary
		Clock deterministicClock() {
			return Clock.fixed(
					Instant.parse("2026-07-25T08:15:30Z"),
					ZoneOffset.UTC);
		}
	}

	static final class FailAfterSaveIncidentRepository
			implements IncidentRepository {

		private final IncidentPersistenceAdapter delegate;
		private boolean failNextSave;

		FailAfterSaveIncidentRepository(IncidentPersistenceAdapter delegate) {
			this.delegate = delegate;
		}

		@Override
		public Incident save(Incident incident) {
			Incident saved = delegate.save(incident);
			if (failNextSave) {
				failNextSave = false;
				throw new SimulatedPersistenceFailure();
			}
			return saved;
		}

		@Override
		public List<Incident> findAll(IncidentCriteria criteria) {
			return delegate.findAll(criteria);
		}

		@Override
		public Optional<Incident> findById(long id) {
			return delegate.findById(id);
		}

		void failNextSave() {
			failNextSave = true;
		}

		void allowSaves() {
			failNextSave = false;
		}
	}

	static final class FailAfterSaveEscalationRepository
			implements EscalationRepository {

		private final EscalationPersistenceAdapter delegate;
		private boolean failNextSave;

		FailAfterSaveEscalationRepository(
				EscalationPersistenceAdapter delegate) {
			this.delegate = delegate;
		}

		@Override
		public Escalation save(Escalation escalation) {
			Escalation saved = delegate.save(escalation);
			if (failNextSave) {
				failNextSave = false;
				throw new SimulatedPersistenceFailure();
			}
			return saved;
		}

		@Override
		public int findHighestLevel(long incidentId) {
			return delegate.findHighestLevel(incidentId);
		}

		void failNextSave() {
			failNextSave = true;
		}

		void allowSaves() {
			failNextSave = false;
		}
	}

	static final class SimulatedPersistenceFailure extends RuntimeException {
	}
}
