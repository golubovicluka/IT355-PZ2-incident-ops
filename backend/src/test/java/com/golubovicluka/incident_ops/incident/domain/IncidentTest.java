package com.golubovicluka.incident_ops.incident.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class IncidentTest {

	private static final Instant CREATED_AT = Instant.parse("2026-07-25T08:15:30Z");
	private static final IncidentManagedService SERVICE =
			new IncidentManagedService(7L, "Payments API");
	private static final IncidentUser REPORTER =
			new IncidentUser(11L, "responder", "Response Engineer");
	private static final IncidentUser ASSIGNEE =
			new IncidentUser(12L, "ana", "Ana Anić");

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

	private Incident persistedIncident() {
		return new Incident(
				42L,
				"INC-20260725-AB12CD34",
				"Checkout failures",
				"Card payments are timing out.",
				IncidentPriority.SEV1,
				IncidentStatus.OPEN,
				SERVICE,
				REPORTER,
				ASSIGNEE,
				CREATED_AT,
				CREATED_AT,
				java.util.List.of(IncidentEvent.created(REPORTER, CREATED_AT)));
	}
}
