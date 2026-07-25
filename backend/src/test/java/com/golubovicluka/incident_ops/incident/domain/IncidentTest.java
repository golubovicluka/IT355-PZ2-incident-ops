package com.golubovicluka.incident_ops.incident.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IncidentTest {

	private static final Instant CREATED_AT = Instant.parse("2026-07-25T08:15:30Z");
	private static final IncidentManagedService SERVICE =
			new IncidentManagedService(7L, "Payments API");
	private static final IncidentUser REPORTER =
			new IncidentUser(11L, "responder", "Response Engineer");
	private static final IncidentUser ASSIGNEE =
			new IncidentUser(12L, "ana", "Ana Anić");
	private static final Instant TRANSITIONED_AT =
			CREATED_AT.plusSeconds(300);

	@Test
	void createsOpenIncidentAndCreatedTimelineEvent() {
		Incident incident = Incident.create(
				"INC-20260725-AB12CD34",
				"  Checkout failures  ",
				"  Card payments are timing out.  ",
				IncidentPriority.SEV1,
				SERVICE,
				REPORTER,
				ASSIGNEE,
				CREATED_AT);

		assertThat(incident.referenceCode()).isEqualTo("INC-20260725-AB12CD34");
		assertThat(incident.title()).isEqualTo("Checkout failures");
		assertThat(incident.description()).isEqualTo("Card payments are timing out.");
		assertThat(incident.priority()).isEqualTo(IncidentPriority.SEV1);
		assertThat(incident.status()).isEqualTo(IncidentStatus.OPEN);
		assertThat(incident.managedService()).isEqualTo(SERVICE);
		assertThat(incident.reporter()).isEqualTo(REPORTER);
		assertThat(incident.assignee()).isEqualTo(ASSIGNEE);
		assertThat(incident.createdAt()).isEqualTo(CREATED_AT);
		assertThat(incident.updatedAt()).isEqualTo(CREATED_AT);
		assertThat(incident.events()).containsExactly(
				IncidentEvent.created(REPORTER, CREATED_AT));
	}

	@Test
	void updatePreservesIdentityReporterStatusAndTimeline() {
		Incident incident = persistedIncident();
		Instant updatedAt = CREATED_AT.plusSeconds(300);
		IncidentManagedService checkout =
				new IncidentManagedService(8L, "Checkout API");

		Incident updated = incident.update(
				"Updated title",
				"Updated description",
				IncidentPriority.SEV2,
				checkout,
				null,
				updatedAt);

		assertThat(updated.id()).isEqualTo(42L);
		assertThat(updated.referenceCode()).isEqualTo(incident.referenceCode());
		assertThat(updated.status()).isEqualTo(IncidentStatus.OPEN);
		assertThat(updated.reporter()).isEqualTo(REPORTER);
		assertThat(updated.createdAt()).isEqualTo(CREATED_AT);
		assertThat(updated.events()).isEqualTo(incident.events());
		assertThat(updated.managedService()).isEqualTo(checkout);
		assertThat(updated.assignee()).isNull();
		assertThat(updated.updatedAt()).isEqualTo(updatedAt);
	}

	@Test
	void rejectsMissingOrOversizedCoreFields() {
		assertThatThrownBy(() -> Incident.create(
				" ",
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				SERVICE,
				REPORTER,
				null,
				CREATED_AT))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("referenceCode must not be blank");

		assertThatThrownBy(() -> Incident.create(
				"INC-1",
				" ",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				SERVICE,
				REPORTER,
				null,
				CREATED_AT))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("title must not be blank");

		assertThatThrownBy(() -> Incident.create(
				"INC-1",
				"Checkout failures",
				"x".repeat(Incident.MAX_DESCRIPTION_LENGTH + 1),
				IncidentPriority.SEV1,
				SERVICE,
				REPORTER,
				null,
				CREATED_AT))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("description must not exceed 4000 characters");
	}

	@ParameterizedTest
	@MethodSource("allowedTransitions")
	void transitionsOnlyThroughAllowedLifecycle(
			IncidentStatus currentStatus,
			IncidentStatus nextStatus) {
		Incident incident = persistedIncident(currentStatus);

		Incident transitioned = incident.transitionTo(
				nextStatus,
				ASSIGNEE,
				TRANSITIONED_AT);

		assertThat(transitioned.status()).isEqualTo(nextStatus);
		assertThat(transitioned.updatedAt()).isEqualTo(TRANSITIONED_AT);
		assertThat(transitioned.events()).hasSize(2);
		assertThat(transitioned.events().getLast())
				.isEqualTo(IncidentEvent.statusChanged(
						ASSIGNEE,
						currentStatus,
						nextStatus,
						TRANSITIONED_AT));
		assertLifecycleTimestamps(transitioned, currentStatus, nextStatus);
	}

	@ParameterizedTest
	@MethodSource("forbiddenTransitions")
	void rejectsEveryNoOpAndForbiddenTransitionWithoutChangingIncident(
			IncidentStatus currentStatus,
			IncidentStatus nextStatus) {
		Incident incident = persistedIncident(currentStatus);

		assertThatThrownBy(() -> incident.transitionTo(
				nextStatus,
				ASSIGNEE,
				TRANSITIONED_AT))
				.isInstanceOf(InvalidIncidentStatusTransitionException.class)
				.hasMessage(
						"Incident status cannot transition from %s to %s",
						currentStatus,
						nextStatus);

		assertThat(incident.status()).isEqualTo(currentStatus);
		assertThat(incident.events()).hasSize(1);
		assertThat(incident.updatedAt()).isEqualTo(lifecycleUpdatedAt(currentStatus));
	}

	@Test
	void addsNormalizedTimelineNoteWithoutChangingLifecycle() {
		Incident incident = persistedIncident();

		Incident noted = incident.addNote(
				"  Rolled back the checkout deployment.  ",
				ASSIGNEE,
				TRANSITIONED_AT);

		assertThat(noted.status()).isEqualTo(IncidentStatus.OPEN);
		assertThat(noted.acknowledgedAt()).isNull();
		assertThat(noted.resolvedAt()).isNull();
		assertThat(noted.updatedAt()).isEqualTo(TRANSITIONED_AT);
		assertThat(noted.events()).hasSize(2);
		assertThat(noted.events().getLast())
				.isEqualTo(IncidentEvent.noteAdded(
						ASSIGNEE,
						"Rolled back the checkout deployment.",
						TRANSITIONED_AT));
	}

	@Test
	void rejectsBlankOrOversizedTimelineNoteWithoutChangingIncident() {
		Incident incident = persistedIncident();

		assertThatThrownBy(() -> incident.addNote(
				"  ",
				ASSIGNEE,
				TRANSITIONED_AT))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("note must not be blank");
		assertThatThrownBy(() -> incident.addNote(
				"x".repeat(IncidentEvent.MAX_NOTE_LENGTH + 1),
				ASSIGNEE,
				TRANSITIONED_AT))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("note must not exceed 2000 characters");

		assertThat(incident.events()).hasSize(1);
		assertThat(incident.updatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void addsServerLevelledEscalationEventToEligibleIncident() {
		Incident incident = persistedIncident(IncidentStatus.INVESTIGATING);

		Incident escalated = incident.addEscalation(
				2,
				"  Customer checkout is unavailable.  ",
				ASSIGNEE,
				TRANSITIONED_AT);

		assertThat(escalated.status()).isEqualTo(IncidentStatus.INVESTIGATING);
		assertThat(escalated.updatedAt()).isEqualTo(TRANSITIONED_AT);
		assertThat(escalated.events().getLast())
				.isEqualTo(IncidentEvent.escalated(
						ASSIGNEE,
						2,
						"Customer checkout is unavailable.",
						TRANSITIONED_AT));
	}

	@ParameterizedTest
	@MethodSource("blockedEscalationStatuses")
	void rejectsEscalationForResolvedAndClosedIncidents(
			IncidentStatus status) {
		Incident incident = persistedIncident(status);

		assertThatThrownBy(() -> incident.addEscalation(
				1,
				"Customer impact requires management attention.",
				ASSIGNEE,
				TRANSITIONED_AT))
				.isInstanceOf(IncidentEscalationNotAllowedException.class)
				.hasMessage(
						"Incident cannot be escalated while its status is %s",
						status);

		assertThat(incident.events()).hasSize(1);
		assertThat(incident.updatedAt()).isEqualTo(
				lifecycleUpdatedAt(status));
	}

	private Incident persistedIncident() {
		return persistedIncident(IncidentStatus.OPEN);
	}

	private Incident persistedIncident(IncidentStatus status) {
		Instant acknowledgedAt = switch (status) {
			case OPEN -> null;
			default -> CREATED_AT.plusSeconds(60);
		};
		Instant resolvedAt = switch (status) {
			case RESOLVED, CLOSED -> CREATED_AT.plusSeconds(120);
			default -> null;
		};
		return new Incident(
				42L,
				"INC-20260725-AB12CD34",
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				status,
				SERVICE,
				REPORTER,
				ASSIGNEE,
				CREATED_AT,
				lifecycleUpdatedAt(status),
				acknowledgedAt,
				resolvedAt,
				java.util.List.of(IncidentEvent.created(REPORTER, CREATED_AT)));
	}

	private static Instant lifecycleUpdatedAt(IncidentStatus status) {
		return switch (status) {
			case OPEN -> CREATED_AT;
			case ACKNOWLEDGED, INVESTIGATING -> CREATED_AT.plusSeconds(60);
			case RESOLVED, CLOSED -> CREATED_AT.plusSeconds(120);
		};
	}

	private void assertLifecycleTimestamps(
			Incident transitioned,
			IncidentStatus previousStatus,
			IncidentStatus nextStatus) {
		if (previousStatus == IncidentStatus.OPEN) {
			assertThat(transitioned.acknowledgedAt()).isEqualTo(TRANSITIONED_AT);
		} else {
			assertThat(transitioned.acknowledgedAt())
					.isEqualTo(CREATED_AT.plusSeconds(60));
		}

		if (nextStatus == IncidentStatus.RESOLVED) {
			assertThat(transitioned.resolvedAt()).isEqualTo(TRANSITIONED_AT);
		} else if (previousStatus == IncidentStatus.RESOLVED
				&& nextStatus == IncidentStatus.INVESTIGATING) {
			assertThat(transitioned.resolvedAt()).isNull();
		} else if (previousStatus == IncidentStatus.RESOLVED) {
			assertThat(transitioned.resolvedAt())
					.isEqualTo(CREATED_AT.plusSeconds(120));
		} else {
			assertThat(transitioned.resolvedAt()).isNull();
		}
	}

	private static Stream<Arguments> allowedTransitions() {
		return Stream.of(
				Arguments.of(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED),
				Arguments.of(IncidentStatus.OPEN, IncidentStatus.INVESTIGATING),
				Arguments.of(
						IncidentStatus.ACKNOWLEDGED,
						IncidentStatus.INVESTIGATING),
				Arguments.of(
						IncidentStatus.INVESTIGATING,
						IncidentStatus.RESOLVED),
				Arguments.of(IncidentStatus.RESOLVED, IncidentStatus.CLOSED),
				Arguments.of(
						IncidentStatus.RESOLVED,
						IncidentStatus.INVESTIGATING));
	}

	private static Stream<Arguments> forbiddenTransitions() {
		Set<String> allowed = Set.of(
				"OPEN->ACKNOWLEDGED",
				"OPEN->INVESTIGATING",
				"ACKNOWLEDGED->INVESTIGATING",
				"INVESTIGATING->RESOLVED",
				"RESOLVED->CLOSED",
				"RESOLVED->INVESTIGATING");
		return Arrays.stream(IncidentStatus.values())
				.flatMap(current -> Arrays.stream(IncidentStatus.values())
						.filter(next -> !allowed.contains(current + "->" + next))
						.map(next -> Arguments.of(current, next)));
	}

	private static Stream<IncidentStatus> blockedEscalationStatuses() {
		return Stream.of(IncidentStatus.RESOLVED, IncidentStatus.CLOSED);
	}
}
