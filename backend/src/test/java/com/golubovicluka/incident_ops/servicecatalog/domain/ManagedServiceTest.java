package com.golubovicluka.incident_ops.servicecatalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ManagedServiceTest {

	@Test
	void createsServiceWithNormalizedTextAndOwningTeam() {
		ManagedService service = ManagedService.create(
				"  Payments API  ",
				"  Processes card payments.  ",
				Criticality.CRITICAL,
				new OwningTeam(7L, "Platform Operations"));

		assertThat(service).isEqualTo(new ManagedService(
				null,
				"Payments API",
				"Processes card payments.",
				Criticality.CRITICAL,
				new OwningTeam(7L, "Platform Operations")));
	}

	@Test
	void rejectsAnOwningTeamThatHasNotBeenPersisted() {
		assertThatThrownBy(() -> new OwningTeam(null, "Platform Operations"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("owning team id must be positive");
	}

	@Test
	void rejectsBlankAndOverlongFields() {
		OwningTeam team = new OwningTeam(7L, "Platform Operations");

		assertThatThrownBy(() -> ManagedService.create(
				" ",
				"Processes payments",
				Criticality.HIGH,
				team))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("name must not be blank");
		assertThatThrownBy(() -> ManagedService.create(
				"Payments API",
				"x".repeat(ManagedService.MAX_DESCRIPTION_LENGTH + 1),
				Criticality.HIGH,
				team))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("description must not exceed 500 characters");
	}
}
