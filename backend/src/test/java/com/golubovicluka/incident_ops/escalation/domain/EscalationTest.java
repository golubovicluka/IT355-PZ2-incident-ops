package com.golubovicluka.incident_ops.escalation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.golubovicluka.incident_ops.incident.domain.IncidentEvent;
import org.junit.jupiter.api.Test;

class EscalationTest {

	private static final EscalationActor ACTOR =
			new EscalationActor(11L, "responder", "Response Engineer");
	private static final Instant ESCALATED_AT =
			Instant.parse("2026-07-25T08:20:30Z");

	@Test
	void normalizesReasonAndCalculatesNextServerLevel() {
		Escalation escalation = Escalation.create(
				42L,
				Escalation.nextLevel(1),
				"  Checkout is unavailable for all customers.  ",
				ACTOR,
				ESCALATED_AT);

		assertThat(escalation.level()).isEqualTo(2);
		assertThat(escalation.reason())
				.isEqualTo("Checkout is unavailable for all customers.");
	}

	@Test
	void rejectsInvalidReasonAndUnusableLevelSequence() {
		assertThatThrownBy(() -> Escalation.create(
				42L,
				1,
				" ",
				ACTOR,
				ESCALATED_AT))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("reason must not be blank");
		assertThatThrownBy(() -> Escalation.create(
				42L,
				1,
				"x".repeat(IncidentEvent.MAX_ESCALATION_REASON_LENGTH + 1),
				ACTOR,
				ESCALATED_AT))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("reason must not exceed 1000 characters");
		assertThatThrownBy(() -> Escalation.nextLevel(Integer.MAX_VALUE))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("highest escalation level cannot be incremented");
	}
}
