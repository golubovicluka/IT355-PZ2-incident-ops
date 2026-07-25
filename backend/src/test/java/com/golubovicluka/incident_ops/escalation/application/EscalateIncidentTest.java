package com.golubovicluka.incident_ops.escalation.application;

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

import com.golubovicluka.incident_ops.escalation.application.command.EscalateIncidentCommand;
import com.golubovicluka.incident_ops.escalation.domain.Escalation;
import com.golubovicluka.incident_ops.escalation.domain.EscalationRepository;
import com.golubovicluka.incident_ops.identity.application.FindAssignableUser;
import com.golubovicluka.incident_ops.identity.application.dto.AssignableUserView;
import com.golubovicluka.incident_ops.incident.application.IncidentActorNotFoundException;
import com.golubovicluka.incident_ops.incident.application.RecordIncidentEscalation;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.IncidentEscalationNotAllowedException;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EscalateIncidentTest {

	private static final Instant NOW =
			Instant.parse("2026-07-25T08:20:30Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Mock
	private EscalationRepository escalations;

	@Mock
	private FindAssignableUser findAssignableUser;

	@Mock
	private RecordIncidentEscalation recordIncidentEscalation;

	private EscalateIncident escalateIncident;

	@BeforeEach
	void setUp() {
		escalateIncident = new EscalateIncident(
				escalations,
				findAssignableUser,
				recordIncidentEscalation,
				CLOCK);
	}

	@Test
	void assignsNextLevelAndRecordsEscalationWithServerActorAndTime() {
		AssignableUserView actor = actor();
		IncidentDetailView detail = detail();
		when(findAssignableUser.byUsername("responder"))
				.thenReturn(Optional.of(actor));
		when(escalations.findHighestLevel(42L)).thenReturn(1);
		when(recordIncidentEscalation.execute(
				42L,
				2,
				"Checkout is unavailable.",
				new IncidentUser(11L, "responder", "Response Engineer"),
				NOW)).thenReturn(detail);
		when(escalations.save(any(Escalation.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		IncidentDetailView result = escalateIncident.execute(
				new EscalateIncidentCommand(
						42L,
						"  Checkout is unavailable.  ",
						"responder"));

		ArgumentCaptor<Escalation> saved =
				ArgumentCaptor.forClass(Escalation.class);
		verify(escalations).save(saved.capture());
		assertThat(saved.getValue().level()).isEqualTo(2);
		assertThat(saved.getValue().actor().username()).isEqualTo("responder");
		assertThat(saved.getValue().escalatedAt()).isEqualTo(NOW);
		assertThat(result).isSameAs(detail);
	}

	@Test
	void failedStatusGuardPersistsNeitherEscalationNorIncidentResult() {
		when(findAssignableUser.byUsername("responder"))
				.thenReturn(Optional.of(actor()));
		when(escalations.findHighestLevel(42L)).thenReturn(0);
		when(recordIncidentEscalation.execute(
				42L,
				1,
				"Checkout is unavailable.",
				new IncidentUser(11L, "responder", "Response Engineer"),
				NOW)).thenThrow(
						new IncidentEscalationNotAllowedException(
								IncidentStatus.RESOLVED));

		assertThatThrownBy(() -> escalateIncident.execute(
				new EscalateIncidentCommand(
						42L,
						"Checkout is unavailable.",
						"responder")))
				.isInstanceOf(IncidentEscalationNotAllowedException.class);

		verify(escalations, never()).save(any(Escalation.class));
	}

	@Test
	void unknownAuthenticatedActorStopsBeforeLevelCalculation() {
		when(findAssignableUser.byUsername("missing"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> escalateIncident.execute(
				new EscalateIncidentCommand(
						42L,
						"Checkout is unavailable.",
						"missing")))
				.isInstanceOf(IncidentActorNotFoundException.class);

		verify(escalations, never()).findHighestLevel(42L);
		verify(recordIncidentEscalation, never()).execute(
				any(Long.class),
				any(Integer.class),
				any(String.class),
				any(IncidentUser.class),
				any(Instant.class));
	}

	private AssignableUserView actor() {
		return new AssignableUserView(
				11L,
				"responder",
				"Response Engineer",
				new AssignableUserView.TeamView(3L, "Platform"));
	}

	private IncidentDetailView detail() {
		IncidentDetailView.UserView actor =
				new IncidentDetailView.UserView(
						11L,
						"responder",
						"Response Engineer");
		return new IncidentDetailView(
				42L,
				"INC-20260725-AB12CD34",
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				IncidentStatus.OPEN,
				new IncidentDetailView.ManagedServiceView(7L, "Payments API"),
				actor,
				null,
				NOW.minusSeconds(300),
				NOW,
				null,
				null,
				List.of(IncidentStatus.ACKNOWLEDGED),
				List.of(new IncidentDetailView.EventView(
						100L,
						com.golubovicluka.incident_ops.incident.domain.IncidentEventKind.ESCALATED,
						actor,
						null,
						null,
						null,
						1,
						"Checkout is unavailable.",
						NOW)));
	}
}
