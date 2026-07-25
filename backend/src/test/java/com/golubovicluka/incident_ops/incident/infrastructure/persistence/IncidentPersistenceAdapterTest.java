package com.golubovicluka.incident_ops.incident.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import com.golubovicluka.incident_ops.incident.domain.DuplicateIncidentReferenceCodeException;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentCriteria;
import com.golubovicluka.incident_ops.incident.domain.IncidentEvent;
import com.golubovicluka.incident_ops.incident.domain.IncidentEventKind;
import com.golubovicluka.incident_ops.incident.domain.IncidentManagedService;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;
import com.golubovicluka.incident_ops.integration.PostgreSQLContainerSupport;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import com.golubovicluka.incident_ops.servicecatalog.domain.OwningTeam;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IncidentPersistenceAdapterTest extends PostgreSQLContainerSupport {

	private static final Instant BASE_TIME =
			Instant.parse("2026-07-25T08:00:00Z");

	@Autowired
	private TeamRepository teams;

	@Autowired
	private UserAccountRepository users;

	@Autowired
	private ManagedServiceRepository services;

	@Autowired
	private IncidentRepository incidents;

	@Autowired
	private EntityManager entityManager;

	private UserAccount reporter;
	private UserAccount assignee;
	private ManagedService payments;
	private ManagedService checkout;

	@BeforeEach
	void setUpCatalogs() {
		Team team = teams.save(Team.create("Platform " + System.nanoTime()));
		reporter = users.save(UserAccount.create(
				"reporter-" + System.nanoTime(),
				"Response Engineer",
				"$2a$10$reporterHash",
				Set.of(Role.RESPONDER),
				team));
		assignee = users.save(UserAccount.create(
				"assignee-" + System.nanoTime(),
				"Ana Anić",
				"$2a$10$assigneeHash",
				Set.of(Role.RESPONDER),
				team));
		payments = services.save(ManagedService.create(
				"Payments " + System.nanoTime(),
				"Processes card payments.",
				Criticality.CRITICAL,
				new OwningTeam(team.id(), team.name())));
		checkout = services.save(ManagedService.create(
				"Checkout " + System.nanoTime(),
				"Coordinates checkout.",
				Criticality.HIGH,
				new OwningTeam(team.id(), team.name())));
	}

	@Test
	void persistsIncidentRelationsAndImmutableCreatedEvent() {
		Incident saved = incidents.save(incident(
				"INC-20260725-0001",
				"Checkout failures",
				IncidentPriority.SEV1,
				IncidentStatus.OPEN,
				payments,
				assignee,
				BASE_TIME));
		entityManager.clear();

		Incident loaded = incidents.findById(saved.id()).orElseThrow();

		assertThat(loaded.id()).isNotNull();
		assertThat(loaded.managedService().id()).isEqualTo(payments.id());
		assertThat(loaded.reporter().id()).isEqualTo(reporter.id());
		assertThat(loaded.assignee().id()).isEqualTo(assignee.id());
		assertThat(loaded.events()).singleElement().satisfies(event -> {
			assertThat(event.id()).isNotNull();
			assertThat(event.kind()).isEqualTo(IncidentEventKind.CREATED);
			assertThat(event.actor().id()).isEqualTo(reporter.id());
			assertThat(event.occurredAt()).isEqualTo(BASE_TIME);
		});
	}

	@Test
	void filtersByStatusPriorityAndServiceWithNewestFirstOrdering() {
		Incident oldest = incidents.save(incident(
				"INC-20260725-0010",
				"Old open incident",
				IncidentPriority.SEV1,
				IncidentStatus.OPEN,
				payments,
				null,
				BASE_TIME));
		Incident newest = incidents.save(incident(
				"INC-20260725-0011",
				"Newest open incident",
				IncidentPriority.SEV2,
				IncidentStatus.OPEN,
				payments,
				null,
				BASE_TIME.plusSeconds(120)));
		Incident middle = incidents.save(incident(
				"INC-20260725-0012",
				"Resolved incident",
				IncidentPriority.SEV1,
				IncidentStatus.RESOLVED,
				checkout,
				null,
				BASE_TIME.plusSeconds(60)));
		entityManager.clear();

		assertThat(incidents.findAll(new IncidentCriteria(null, null, null)))
				.extracting(Incident::id)
				.containsExactly(newest.id(), middle.id(), oldest.id());
		assertThat(incidents.findAll(new IncidentCriteria(
				IncidentStatus.OPEN,
				null,
				null)))
				.extracting(Incident::id)
				.containsExactly(newest.id(), oldest.id());
		assertThat(incidents.findAll(new IncidentCriteria(
				null,
				IncidentPriority.SEV1,
				null)))
				.extracting(Incident::id)
				.containsExactly(middle.id(), oldest.id());
		assertThat(incidents.findAll(new IncidentCriteria(
				null,
				null,
				payments.id())))
				.extracting(Incident::id)
				.containsExactly(newest.id(), oldest.id());
		assertThat(incidents.findAll(new IncidentCriteria(
				IncidentStatus.OPEN,
				IncidentPriority.SEV1,
				payments.id())))
				.extracting(Incident::id)
				.containsExactly(oldest.id());
	}

	@Test
	void updatesEditableFieldsWithoutChangingCreatedEvent() {
		Incident saved = incidents.save(incident(
				"INC-20260725-0020",
				"Original title",
				IncidentPriority.SEV1,
				IncidentStatus.OPEN,
				payments,
				null,
				BASE_TIME));
		Long eventId = saved.events().getFirst().id();

		Incident updated = incidents.save(saved.update(
				"Updated title",
				"Updated description",
				IncidentPriority.SEV2,
				new IncidentManagedService(checkout.id(), checkout.name()),
				toUser(assignee),
				BASE_TIME.plusSeconds(300)));
		entityManager.clear();
		Incident reloaded = incidents.findById(updated.id()).orElseThrow();

		assertThat(reloaded.title()).isEqualTo("Updated title");
		assertThat(reloaded.description()).isEqualTo("Updated description");
		assertThat(reloaded.priority()).isEqualTo(IncidentPriority.SEV2);
		assertThat(reloaded.managedService().id()).isEqualTo(checkout.id());
		assertThat(reloaded.assignee().id()).isEqualTo(assignee.id());
		assertThat(reloaded.events()).singleElement()
				.extracting(IncidentEvent::id)
				.isEqualTo(eventId);
	}

	@Test
	void persistsLifecycleTimestampsAndImmutableStatusEvent() {
		Incident saved = incidents.save(incident(
				"INC-20260725-STATUS",
				"Lifecycle incident",
				IncidentPriority.SEV1,
				IncidentStatus.OPEN,
				payments,
				assignee,
				BASE_TIME));
		Instant transitionedAt = BASE_TIME.plusSeconds(300);

		Incident transitioned = incidents.save(saved.transitionTo(
				IncidentStatus.INVESTIGATING,
				toUser(assignee),
				transitionedAt));
		entityManager.clear();

		Incident loaded = incidents.findById(transitioned.id()).orElseThrow();
		assertThat(loaded.status()).isEqualTo(IncidentStatus.INVESTIGATING);
		assertThat(loaded.acknowledgedAt()).isEqualTo(transitionedAt);
		assertThat(loaded.resolvedAt()).isNull();
		assertThat(loaded.updatedAt()).isEqualTo(transitionedAt);
		assertThat(loaded.events()).hasSize(2);
		assertThat(loaded.events().getLast()).satisfies(event -> {
			assertThat(event.id()).isNotNull();
			assertThat(event.kind())
					.isEqualTo(IncidentEventKind.STATUS_CHANGED);
			assertThat(event.actor().id()).isEqualTo(assignee.id());
			assertThat(event.previousStatus()).isEqualTo(IncidentStatus.OPEN);
			assertThat(event.newStatus())
					.isEqualTo(IncidentStatus.INVESTIGATING);
			assertThat(event.occurredAt()).isEqualTo(transitionedAt);
		});
	}

	@Test
	void databaseConstraintRejectsDuplicateReferenceCode() {
		incidents.save(incident(
				"INC-20260725-DUPLICATE",
				"First incident",
				IncidentPriority.SEV3,
				IncidentStatus.OPEN,
				payments,
				null,
				BASE_TIME));

		assertThatThrownBy(() -> incidents.save(incident(
				"INC-20260725-DUPLICATE",
				"Second incident",
				IncidentPriority.SEV4,
				IncidentStatus.OPEN,
				checkout,
				null,
				BASE_TIME.plusSeconds(1))))
				.isInstanceOf(DuplicateIncidentReferenceCodeException.class);
	}

	private Incident incident(
			String referenceCode,
			String title,
			IncidentPriority priority,
			IncidentStatus status,
			ManagedService service,
			UserAccount assignedUser,
			Instant createdAt) {
		IncidentUser reporterUser = toUser(reporter);
		return new Incident(
				null,
				referenceCode,
				title,
				"Incident description.",
				priority,
				status,
				new IncidentManagedService(service.id(), service.name()),
				reporterUser,
				assignedUser == null ? null : toUser(assignedUser),
				createdAt,
				createdAt,
				status == IncidentStatus.OPEN
						? null
						: createdAt,
				status == IncidentStatus.RESOLVED
						|| status == IncidentStatus.CLOSED
								? createdAt
								: null,
				List.of(IncidentEvent.created(reporterUser, createdAt)));
	}

	private IncidentUser toUser(UserAccount account) {
		return new IncidentUser(
				account.id(),
				account.username(),
				account.displayName());
	}
}
