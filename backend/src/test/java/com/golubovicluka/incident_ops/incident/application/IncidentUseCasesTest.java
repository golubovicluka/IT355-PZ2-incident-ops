package com.golubovicluka.incident_ops.incident.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.identity.application.FindAssignableUser;
import com.golubovicluka.incident_ops.identity.application.dto.AssignableUserView;
import com.golubovicluka.incident_ops.incident.application.command.AddIncidentNoteCommand;
import com.golubovicluka.incident_ops.incident.application.command.ChangeIncidentStatusCommand;
import com.golubovicluka.incident_ops.incident.application.command.CreateIncidentCommand;
import com.golubovicluka.incident_ops.incident.application.command.UpdateIncidentCommand;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentCriteria;
import com.golubovicluka.incident_ops.incident.domain.IncidentEvent;
import com.golubovicluka.incident_ops.incident.domain.IncidentManagedService;
import com.golubovicluka.incident_ops.incident.domain.IncidentNotFoundException;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;
import com.golubovicluka.incident_ops.incident.domain.InvalidIncidentStatusTransitionException;
import com.golubovicluka.incident_ops.servicecatalog.application.FindManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentUseCasesTest {

	private static final Instant NOW = Instant.parse("2026-07-25T08:15:30Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Mock
	private IncidentRepository incidents;

	@Mock
	private FindManagedService findManagedService;

	@Mock
	private FindAssignableUser findAssignableUser;

	@Mock
	private ReferenceCodeGenerator referenceCodeGenerator;

	private CreateIncident createIncident;
	private AddIncidentNote addIncidentNote;
	private ChangeIncidentStatus changeIncidentStatus;
	private GetIncident getIncident;
	private ListIncidents listIncidents;
	private UpdateIncident updateIncident;

	@BeforeEach
	void setUp() {
		createIncident = new CreateIncident(
				incidents,
				findManagedService,
				findAssignableUser,
				referenceCodeGenerator,
				CLOCK);
		addIncidentNote = new AddIncidentNote(
				incidents,
				findAssignableUser,
				CLOCK);
		changeIncidentStatus = new ChangeIncidentStatus(
				incidents,
				findAssignableUser,
				CLOCK);
		getIncident = new GetIncident(incidents);
		listIncidents = new ListIncidents(incidents);
		updateIncident = new UpdateIncident(
				incidents,
				findManagedService,
				findAssignableUser,
				CLOCK);
	}

	@Test
	void createsIncidentFromGeneratedReferenceAndAuthenticatedUsername() {
		when(referenceCodeGenerator.nextReferenceCode())
				.thenReturn("INC-20260725-AB12CD34");
		when(findManagedService.execute(7L)).thenReturn(Optional.of(serviceView()));
		when(findAssignableUser.byUsername("responder"))
				.thenReturn(Optional.of(reporterView()));
		when(findAssignableUser.byId(12L)).thenReturn(Optional.of(assigneeView()));
		when(incidents.save(any(Incident.class)))
				.thenAnswer(invocation -> persist(invocation.getArgument(0)));

		IncidentDetailView created = createIncident.execute(
				new CreateIncidentCommand(
						"  Checkout failures  ",
						"  Card payments are timing out.  ",
						IncidentPriority.SEV1,
						7L,
						12L,
						"responder"));

		ArgumentCaptor<Incident> savedIncident =
				ArgumentCaptor.forClass(Incident.class);
		verify(incidents).save(savedIncident.capture());
		assertThat(savedIncident.getValue().referenceCode())
				.isEqualTo("INC-20260725-AB12CD34");
		assertThat(savedIncident.getValue().reporter().username())
				.isEqualTo("responder");
		assertThat(savedIncident.getValue().assignee().id()).isEqualTo(12L);
		assertThat(savedIncident.getValue().events())
				.containsExactly(IncidentEvent.created(
						savedIncident.getValue().reporter(),
						NOW));
		assertThat(created.status()).isEqualTo(IncidentStatus.OPEN);
		assertThat(created.timeline()).hasSize(1);
		assertThat(created.timeline().getFirst().actor().username())
				.isEqualTo("responder");
	}

	@Test
	void createsUnassignedIncidentWithoutLookingUpAssignee() {
		when(referenceCodeGenerator.nextReferenceCode()).thenReturn("INC-1");
		when(findManagedService.execute(7L)).thenReturn(Optional.of(serviceView()));
		when(findAssignableUser.byUsername("responder"))
				.thenReturn(Optional.of(reporterView()));
		when(incidents.save(any(Incident.class)))
				.thenAnswer(invocation -> persist(invocation.getArgument(0)));

		IncidentDetailView created = createIncident.execute(
				new CreateIncidentCommand(
						"Checkout failures",
						"Card payments are timing out.",
						IncidentPriority.SEV2,
						7L,
						null,
						"responder"));

		assertThat(created.assignee()).isNull();
		verify(findAssignableUser, never()).byId(any(Long.class));
	}

	@Test
	void rejectsUnknownServiceReporterOrAssigneeBeforeSaving() {
		when(findManagedService.execute(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> createIncident.execute(new CreateIncidentCommand(
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				404L,
				null,
				"responder")))
				.isInstanceOf(IncidentManagedServiceNotFoundException.class);

		when(findManagedService.execute(7L)).thenReturn(Optional.of(serviceView()));
		when(findAssignableUser.byUsername("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> createIncident.execute(new CreateIncidentCommand(
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				7L,
				null,
				"missing")))
				.isInstanceOf(IncidentReporterNotFoundException.class);

		when(findAssignableUser.byUsername("responder"))
				.thenReturn(Optional.of(reporterView()));
		when(findAssignableUser.byId(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> createIncident.execute(new CreateIncidentCommand(
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				7L,
				404L,
				"responder")))
				.isInstanceOf(IncidentAssigneeNotFoundException.class);

		verify(incidents, never()).save(any(Incident.class));
	}

	@Test
	void listsUsingAllFiltersAndGetsDetail() {
		Incident incident = persistedIncident();
		IncidentCriteria criteria = new IncidentCriteria(
				IncidentStatus.OPEN,
				IncidentPriority.SEV1,
				7L);
		when(incidents.findAll(criteria)).thenReturn(List.of(incident));
		when(incidents.findById(42L)).thenReturn(Optional.of(incident));

		assertThat(listIncidents.execute(criteria)).singleElement()
				.satisfies(summary -> {
					assertThat(summary.referenceCode())
							.isEqualTo("INC-20260725-AB12CD34");
					assertThat(summary).hasNoNullFieldsOrPropertiesExcept("assignee");
				});
		assertThat(getIncident.execute(42L).timeline()).hasSize(1);
	}

	@Test
	void updateKeepsReporterAndTimelineWhileReplacingEditableFields() {
		Incident existing = persistedIncident();
		when(incidents.findById(42L)).thenReturn(Optional.of(existing));
		when(findManagedService.execute(8L)).thenReturn(Optional.of(
				new ManagedServiceView(
						8L,
						"Checkout API",
						"Coordinates checkout.",
						Criticality.HIGH,
						new ManagedServiceView.TeamView(3L, "Platform"))));
		when(findAssignableUser.byId(12L)).thenReturn(Optional.of(assigneeView()));
		when(incidents.save(any(Incident.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		IncidentDetailView updated = updateIncident.execute(
				new UpdateIncidentCommand(
						42L,
						"Updated title",
						"Updated description",
						IncidentPriority.SEV2,
						8L,
						12L));

		assertThat(updated.referenceCode()).isEqualTo(existing.referenceCode());
		assertThat(updated.reporter().username()).isEqualTo("responder");
		assertThat(updated.timeline()).hasSize(1);
		assertThat(updated.managedService().id()).isEqualTo(8L);
		assertThat(updated.assignee().id()).isEqualTo(12L);
		assertThat(updated.updatedAt()).isEqualTo(NOW);
	}

	@Test
	void changesStatusWithAuthenticatedActorAndServerTime() {
		Incident existing = persistedIncident();
		when(incidents.findById(42L)).thenReturn(Optional.of(existing));
		when(findAssignableUser.byUsername("ana"))
				.thenReturn(Optional.of(assigneeView()));
		when(incidents.save(any(Incident.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		IncidentDetailView updated = changeIncidentStatus.execute(
				new ChangeIncidentStatusCommand(
						42L,
						IncidentStatus.ACKNOWLEDGED,
						"ana"));

		assertThat(updated.status()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
		assertThat(updated.acknowledgedAt()).isEqualTo(NOW);
		assertThat(updated.resolvedAt()).isNull();
		assertThat(updated.allowedTransitions())
				.containsExactly(IncidentStatus.INVESTIGATING);
		assertThat(updated.timeline().getLast()).satisfies(event -> {
			assertThat(event.kind())
					.isEqualTo(com.golubovicluka.incident_ops.incident.domain.IncidentEventKind.STATUS_CHANGED);
			assertThat(event.actor().username()).isEqualTo("ana");
			assertThat(event.previousStatus()).isEqualTo(IncidentStatus.OPEN);
			assertThat(event.newStatus())
					.isEqualTo(IncidentStatus.ACKNOWLEDGED);
			assertThat(event.occurredAt()).isEqualTo(NOW);
		});
	}

	@Test
	void rejectedStatusTransitionDoesNotSaveIncident() {
		Incident existing = persistedIncident();
		when(incidents.findById(42L)).thenReturn(Optional.of(existing));
		when(findAssignableUser.byUsername("ana"))
				.thenReturn(Optional.of(assigneeView()));

		assertThatThrownBy(() -> changeIncidentStatus.execute(
				new ChangeIncidentStatusCommand(
						42L,
						IncidentStatus.CLOSED,
						"ana")))
				.isInstanceOf(InvalidIncidentStatusTransitionException.class)
				.hasMessage(
						"Incident status cannot transition from OPEN to CLOSED");

		verify(incidents, never()).save(any(Incident.class));
		assertThat(existing.status()).isEqualTo(IncidentStatus.OPEN);
		assertThat(existing.events()).hasSize(1);
	}

	@Test
	void statusChangeRejectsUnknownIncidentOrAuthenticatedActor() {
		when(incidents.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> changeIncidentStatus.execute(
				new ChangeIncidentStatusCommand(
						404L,
						IncidentStatus.ACKNOWLEDGED,
						"ana")))
				.isInstanceOf(IncidentNotFoundException.class);

		when(incidents.findById(42L)).thenReturn(Optional.of(persistedIncident()));
		when(findAssignableUser.byUsername("missing"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> changeIncidentStatus.execute(
				new ChangeIncidentStatusCommand(
						42L,
						IncidentStatus.ACKNOWLEDGED,
						"missing")))
				.isInstanceOf(IncidentActorNotFoundException.class);

		verify(incidents, never()).save(any(Incident.class));
	}

	@Test
	void addsNoteWithAuthenticatedActorAndServerTime() {
		Incident existing = persistedIncident();
		when(incidents.findById(42L)).thenReturn(Optional.of(existing));
		when(findAssignableUser.byUsername("ana"))
				.thenReturn(Optional.of(assigneeView()));
		when(incidents.save(any(Incident.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		IncidentDetailView updated = addIncidentNote.execute(
				new AddIncidentNoteCommand(
						42L,
						"  Rolled back the checkout deployment.  ",
						"ana"));

		assertThat(updated.status()).isEqualTo(IncidentStatus.OPEN);
		assertThat(updated.updatedAt()).isEqualTo(NOW);
		assertThat(updated.timeline().getLast()).satisfies(event -> {
			assertThat(event.kind())
					.isEqualTo(com.golubovicluka.incident_ops.incident.domain.IncidentEventKind.NOTE_ADDED);
			assertThat(event.actor().username()).isEqualTo("ana");
			assertThat(event.note())
					.isEqualTo("Rolled back the checkout deployment.");
			assertThat(event.previousStatus()).isNull();
			assertThat(event.newStatus()).isNull();
			assertThat(event.occurredAt()).isEqualTo(NOW);
		});
	}

	@Test
	void rejectedNoteDoesNotSaveIncidentOrAppendEvent() {
		Incident existing = persistedIncident();
		when(incidents.findById(42L)).thenReturn(Optional.of(existing));
		when(findAssignableUser.byUsername("ana"))
				.thenReturn(Optional.of(assigneeView()));

		assertThatThrownBy(() -> addIncidentNote.execute(
				new AddIncidentNoteCommand(42L, " ", "ana")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("note must not be blank");
		assertThatThrownBy(() -> addIncidentNote.execute(
				new AddIncidentNoteCommand(
						42L,
						"x".repeat(IncidentEvent.MAX_NOTE_LENGTH + 1),
						"ana")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("note must not exceed 2000 characters");

		verify(incidents, never()).save(any(Incident.class));
		assertThat(existing.events()).hasSize(1);
	}

	@Test
	void noteRejectsUnknownIncidentOrAuthenticatedActor() {
		when(incidents.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> addIncidentNote.execute(
				new AddIncidentNoteCommand(404L, "Investigating.", "ana")))
				.isInstanceOf(IncidentNotFoundException.class);

		when(incidents.findById(42L)).thenReturn(Optional.of(persistedIncident()));
		when(findAssignableUser.byUsername("missing"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> addIncidentNote.execute(
				new AddIncidentNoteCommand(
						42L,
						"Investigating.",
						"missing")))
				.isInstanceOf(IncidentActorNotFoundException.class);

		verify(incidents, never()).save(any(Incident.class));
	}

	@Test
	void reportsUnknownIncidentForDetailAndUpdate() {
		when(incidents.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> getIncident.execute(404L))
				.isInstanceOf(IncidentNotFoundException.class);
		assertThatThrownBy(() -> updateIncident.execute(
				new UpdateIncidentCommand(
						404L,
						"Missing",
						"Missing incident.",
						IncidentPriority.SEV4,
						7L,
						null)))
				.isInstanceOf(IncidentNotFoundException.class);
	}

	private Incident persist(Incident incident) {
		IncidentEvent event = incident.events().getFirst();
		return new Incident(
				42L,
				incident.referenceCode(),
				incident.title(),
				incident.description(),
				incident.priority(),
				incident.status(),
				incident.managedService(),
				incident.reporter(),
				incident.assignee(),
				incident.createdAt(),
				incident.updatedAt(),
				incident.acknowledgedAt(),
				incident.resolvedAt(),
				List.of(new IncidentEvent(
						99L,
						event.kind(),
						event.actor(),
						event.previousStatus(),
						event.newStatus(),
						event.note(),
						event.occurredAt())));
	}

	private Incident persistedIncident() {
		IncidentUser reporter =
				new IncidentUser(11L, "responder", "Response Engineer");
		return new Incident(
				42L,
				"INC-20260725-AB12CD34",
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				IncidentStatus.OPEN,
				new IncidentManagedService(7L, "Payments API"),
				reporter,
				null,
				NOW.minusSeconds(600),
				NOW.minusSeconds(600),
				null,
				null,
				List.of(new IncidentEvent(
						99L,
						com.golubovicluka.incident_ops.incident.domain.IncidentEventKind.CREATED,
						reporter,
						null,
						null,
						null,
						NOW.minusSeconds(600))));
	}

	private ManagedServiceView serviceView() {
		return new ManagedServiceView(
				7L,
				"Payments API",
				"Processes card payments.",
				Criticality.CRITICAL,
				new ManagedServiceView.TeamView(3L, "Platform"));
	}

	private AssignableUserView reporterView() {
		return new AssignableUserView(
				11L,
				"responder",
				"Response Engineer",
				new AssignableUserView.TeamView(3L, "Platform"));
	}

	private AssignableUserView assigneeView() {
		return new AssignableUserView(
				12L,
				"ana",
				"Ana Anić",
				new AssignableUserView.TeamView(3L, "Platform"));
	}
}
