package com.golubovicluka.incident_ops.escalation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import org.junit.jupiter.api.Test;

class EscalationPolicyTest {

	private static final PolicyManagedService PAYMENTS =
			new PolicyManagedService(7L, "Payments API");

	@Test
	void createsPolicyForServiceAndPriorityWithNormalizedDeadlines() {
		EscalationPolicy policy = EscalationPolicy.create(
				PAYMENTS,
				IncidentPriority.SEV1,
				Duration.ofMinutes(10),
				Duration.ofMinutes(45));

		assertThat(policy).isEqualTo(new EscalationPolicy(
				null,
				PAYMENTS,
				IncidentPriority.SEV1,
				Duration.ofMinutes(10),
				Duration.ofMinutes(45)));
	}

	@Test
	void rejectsNonPositiveDeadlines() {
		assertThatThrownBy(() -> EscalationPolicy.create(
				PAYMENTS,
				IncidentPriority.SEV2,
				Duration.ZERO,
				Duration.ofMinutes(30)))
				.isInstanceOf(InvalidEscalationPolicyDeadlineException.class)
				.hasMessage("Acknowledgement deadline must be positive");

		assertThatThrownBy(() -> EscalationPolicy.create(
				PAYMENTS,
				IncidentPriority.SEV2,
				Duration.ofMinutes(5),
				Duration.ofMinutes(-1)))
				.isInstanceOf(InvalidEscalationPolicyDeadlineException.class)
				.hasMessage("Resolution deadline must be positive");
	}

	@Test
	void rejectsAcknowledgementDeadlineAfterResolutionDeadline() {
		assertThatThrownBy(() -> EscalationPolicy.create(
				PAYMENTS,
				IncidentPriority.SEV3,
				Duration.ofMinutes(60),
				Duration.ofMinutes(45)))
				.isInstanceOf(InvalidEscalationPolicyDeadlineException.class)
				.hasMessage(
						"Acknowledgement deadline must not exceed the resolution deadline");
	}

	@Test
	void rejectsSubMinuteDeadlinesThatCannotBeRepresentedByTheApi() {
		assertThatThrownBy(() -> EscalationPolicy.create(
				PAYMENTS,
				IncidentPriority.SEV4,
				Duration.ofSeconds(30),
				Duration.ofMinutes(5)))
				.isInstanceOf(InvalidEscalationPolicyDeadlineException.class)
				.hasMessage("Acknowledgement deadline must use whole minutes");
	}

	@Test
	void updatesDeadlinesWithoutChangingIdentityOrPair() {
		EscalationPolicy existing = new EscalationPolicy(
				42L,
				PAYMENTS,
				IncidentPriority.SEV1,
				Duration.ofMinutes(10),
				Duration.ofMinutes(45));

		EscalationPolicy updated = existing.update(
				new PolicyManagedService(8L, "Checkout API"),
				IncidentPriority.SEV2,
				Duration.ofMinutes(15),
				Duration.ofMinutes(90));

		assertThat(updated).isEqualTo(new EscalationPolicy(
				42L,
				new PolicyManagedService(8L, "Checkout API"),
				IncidentPriority.SEV2,
				Duration.ofMinutes(15),
				Duration.ofMinutes(90)));
	}
}
