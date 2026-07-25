package com.golubovicluka.incident_ops.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.analytics.domain.SlaPhase;
import com.golubovicluka.incident_ops.analytics.domain.SlaState;
import com.golubovicluka.incident_ops.escalation.application.FindEscalationPolicy;
import com.golubovicluka.incident_ops.escalation.application.dto.EscalationPolicyView;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentEvent;
import com.golubovicluka.incident_ops.incident.domain.IncidentManagedService;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluateIncidentSlaTest {

	private static final Instant CREATED_AT =
			Instant.parse("2026-07-25T08:00:00Z");
	private static final Instant NOW =
			Instant.parse("2026-07-25T08:12:00Z");

	@Mock
	private FindEscalationPolicy findPolicy;

	private EvaluateIncidentSla evaluateSla;

	@BeforeEach
	void setUp() {
		evaluateSla = new EvaluateIncidentSla(
				findPolicy,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void evaluatesMatchingServicePriorityPolicyUsingServerClock() {
		when(findPolicy.execute(7L, IncidentPriority.SEV1))
				.thenReturn(Optional.of(new EscalationPolicyView(
						3L,
						new EscalationPolicyView.ManagedServiceView(
								7L,
								"Payments API"),
						IncidentPriority.SEV1,
						10,
						60)));

		var evaluation = evaluateSla.evaluate(incident());

		assertThat(evaluation.state()).isEqualTo(SlaState.BREACHED);
		assertThat(evaluation.phase()).isEqualTo(SlaPhase.ACKNOWLEDGEMENT);
		assertThat(evaluation.deadline())
				.isEqualTo(CREATED_AT.plusSeconds(600));
	}

	@Test
	void exposesNotConfiguredWhenNoMatchingPolicyExists() {
		when(findPolicy.execute(7L, IncidentPriority.SEV1))
				.thenReturn(Optional.empty());

		var evaluation = evaluateSla.evaluate(incident());

		assertThat(evaluation.state()).isEqualTo(SlaState.NOT_CONFIGURED);
		assertThat(evaluation.phase()).isNull();
		assertThat(evaluation.deadline()).isNull();
	}

	private Incident incident() {
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
				CREATED_AT,
				CREATED_AT,
				null,
				null,
				List.of(IncidentEvent.created(reporter, CREATED_AT)));
	}
}
