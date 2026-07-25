package com.golubovicluka.incident_ops.analytics.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentEvent;
import com.golubovicluka.incident_ops.incident.domain.IncidentManagedService;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IncidentSlaEvaluatorTest {

	private static final Instant CREATED_AT =
			Instant.parse("2026-07-25T08:00:00Z");
	private static final Duration ACK_TARGET = Duration.ofMinutes(10);
	private static final Duration RESOLUTION_TARGET = Duration.ofMinutes(60);
	private final IncidentSlaEvaluator evaluator = new IncidentSlaEvaluator();

	@Test
	void returnsExplicitNotConfiguredStateWithoutDeadline() {
		SlaEvaluation evaluation = evaluator.evaluate(
				IncidentStatus.OPEN,
				CREATED_AT,
				null,
				null,
				null,
				null,
				CREATED_AT.plusSeconds(60));

		assertThat(evaluation).isEqualTo(SlaEvaluation.notConfigured());
	}

	@ParameterizedTest
	@MethodSource("openDeadlineCases")
	void evaluatesOpenAcknowledgementBeforeOnAndAfterDeadline(
			Instant now,
			SlaState expectedState) {
		SlaEvaluation evaluation = evaluator.evaluate(
				IncidentStatus.OPEN,
				CREATED_AT,
				null,
				null,
				ACK_TARGET,
				RESOLUTION_TARGET,
				now);

		assertThat(evaluation.state()).isEqualTo(expectedState);
		assertThat(evaluation.phase()).isEqualTo(SlaPhase.ACKNOWLEDGEMENT);
		assertThat(evaluation.deadline())
				.isEqualTo(CREATED_AT.plus(ACK_TARGET));
	}

	@ParameterizedTest
	@MethodSource("activeResolutionCases")
	void evaluatesAcknowledgedResolutionDeadline(
			Instant now,
			SlaState expectedState) {
		SlaEvaluation evaluation = evaluator.evaluate(
				IncidentStatus.ACKNOWLEDGED,
				CREATED_AT,
				CREATED_AT.plus(Duration.ofMinutes(5)),
				null,
				ACK_TARGET,
				RESOLUTION_TARGET,
				now);

		assertThat(evaluation.state()).isEqualTo(expectedState);
		assertThat(evaluation.phase()).isEqualTo(SlaPhase.RESOLUTION);
		assertThat(evaluation.deadline())
				.isEqualTo(CREATED_AT.plus(RESOLUTION_TARGET));
	}

	@Test
	void evaluatesAResolvedThenReopenedIncidentAgainstResolutionDeadline() {
		IncidentUser responder = new IncidentUser(
				11L,
				"responder",
				"Response Engineer");
		Instant acknowledgedAt = CREATED_AT.plus(Duration.ofMinutes(5));
		Instant resolvedAt = CREATED_AT.plus(Duration.ofMinutes(40));
		Incident resolved = new Incident(
				42L,
				"INC-20260725-AB12CD34",
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				IncidentStatus.RESOLVED,
				new IncidentManagedService(7L, "Payments API"),
				responder,
				responder,
				CREATED_AT,
				resolvedAt,
				acknowledgedAt,
				resolvedAt,
				List.of(IncidentEvent.created(responder, CREATED_AT)));
		Incident reopened = resolved.transitionTo(
				IncidentStatus.INVESTIGATING,
				responder,
				resolvedAt.plus(Duration.ofMinutes(5)));

		SlaEvaluation evaluation = evaluator.evaluate(
				reopened.status(),
				reopened.createdAt(),
				reopened.acknowledgedAt(),
				reopened.resolvedAt(),
				ACK_TARGET,
				RESOLUTION_TARGET,
				CREATED_AT.plus(Duration.ofMinutes(61)));

		assertThat(reopened.resolvedAt()).isNull();
		assertThat(evaluation).isEqualTo(SlaEvaluation.breached(
				SlaPhase.RESOLUTION,
				CREATED_AT.plus(RESOLUTION_TARGET)));
	}

	@Test
	void keepsLateAcknowledgementAsHistoricalBreach() {
		SlaEvaluation evaluation = evaluator.evaluate(
				IncidentStatus.RESOLVED,
				CREATED_AT,
				CREATED_AT.plus(Duration.ofMinutes(11)),
				CREATED_AT.plus(Duration.ofMinutes(40)),
				ACK_TARGET,
				RESOLUTION_TARGET,
				CREATED_AT.plus(Duration.ofMinutes(45)));

		assertThat(evaluation).isEqualTo(SlaEvaluation.breached(
				SlaPhase.ACKNOWLEDGEMENT,
				CREATED_AT.plus(ACK_TARGET)));
	}

	@ParameterizedTest
	@MethodSource("resolvedDeadlineCases")
	void evaluatesResolvedIncidentAgainstActualResolutionTime(
			Instant resolvedAt,
			SlaState expectedState) {
		SlaEvaluation evaluation = evaluator.evaluate(
				IncidentStatus.RESOLVED,
				CREATED_AT,
				CREATED_AT.plus(Duration.ofMinutes(5)),
				resolvedAt,
				ACK_TARGET,
				RESOLUTION_TARGET,
				CREATED_AT.plus(Duration.ofMinutes(90)));

		assertThat(evaluation.state()).isEqualTo(expectedState);
		assertThat(evaluation.phase()).isEqualTo(SlaPhase.RESOLUTION);
		assertThat(evaluation.deadline())
				.isEqualTo(CREATED_AT.plus(RESOLUTION_TARGET));
	}

	private static Stream<Arguments> openDeadlineCases() {
		Instant deadline = CREATED_AT.plus(ACK_TARGET);
		return Stream.of(
				Arguments.of(deadline.minusNanos(1), SlaState.ON_TRACK),
				Arguments.of(deadline, SlaState.ON_TRACK),
				Arguments.of(deadline.plusNanos(1), SlaState.BREACHED));
	}

	private static Stream<Arguments> activeResolutionCases() {
		Instant deadline = CREATED_AT.plus(RESOLUTION_TARGET);
		return Stream.of(
				Arguments.of(deadline.minusNanos(1), SlaState.ON_TRACK),
				Arguments.of(deadline, SlaState.ON_TRACK),
				Arguments.of(deadline.plusNanos(1), SlaState.BREACHED));
	}

	private static Stream<Arguments> resolvedDeadlineCases() {
		Instant deadline = CREATED_AT.plus(RESOLUTION_TARGET);
		return Stream.of(
				Arguments.of(deadline.minusNanos(1), SlaState.MET),
				Arguments.of(deadline, SlaState.MET),
				Arguments.of(deadline.plusNanos(1), SlaState.BREACHED));
	}
}
