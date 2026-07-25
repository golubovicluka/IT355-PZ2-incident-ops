package com.golubovicluka.incident_ops.escalation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.escalation.application.command.CreateEscalationPolicyCommand;
import com.golubovicluka.incident_ops.escalation.application.command.UpdateEscalationPolicyCommand;
import com.golubovicluka.incident_ops.escalation.application.dto.EscalationPolicyView;
import com.golubovicluka.incident_ops.escalation.domain.DuplicateEscalationPolicyException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicy;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyNotFoundException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyRepository;
import com.golubovicluka.incident_ops.escalation.domain.PolicyManagedService;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.servicecatalog.application.FindManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EscalationPolicyUseCasesTest {

	@Mock
	private EscalationPolicyRepository policies;

	@Mock
	private FindManagedService findManagedService;

	private CreateEscalationPolicy createPolicy;
	private ListEscalationPolicies listPolicies;
	private UpdateEscalationPolicy updatePolicy;
	private DeleteEscalationPolicy deletePolicy;

	@BeforeEach
	void setUp() {
		createPolicy = new CreateEscalationPolicy(policies, findManagedService);
		listPolicies = new ListEscalationPolicies(policies);
		updatePolicy = new UpdateEscalationPolicy(policies, findManagedService);
		deletePolicy = new DeleteEscalationPolicy(policies);
	}

	@Test
	void createsPolicyForAnExistingServiceAndUnusedPriority() {
		when(findManagedService.execute(7L)).thenReturn(Optional.of(serviceView()));
		when(policies.findByManagedServiceIdAndPriority(
				7L,
				IncidentPriority.SEV1)).thenReturn(Optional.empty());
		when(policies.save(any(EscalationPolicy.class))).thenReturn(policy());

		EscalationPolicyView created = createPolicy.execute(
				new CreateEscalationPolicyCommand(
						7L,
						IncidentPriority.SEV1,
						Duration.ofMinutes(10),
						Duration.ofMinutes(45)));

		assertThat(created).isEqualTo(view());
	}

	@Test
	void rejectsCreationForMissingServiceOrDuplicatePair() {
		when(findManagedService.execute(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> createPolicy.execute(
				new CreateEscalationPolicyCommand(
						404L,
						IncidentPriority.SEV1,
						Duration.ofMinutes(10),
						Duration.ofMinutes(45))))
				.isInstanceOf(PolicyManagedServiceNotFoundException.class)
				.hasMessage("Managed service does not exist");

		when(findManagedService.execute(7L)).thenReturn(Optional.of(serviceView()));
		when(policies.findByManagedServiceIdAndPriority(
				7L,
				IncidentPriority.SEV1)).thenReturn(Optional.of(policy()));

		assertThatThrownBy(() -> createPolicy.execute(
				new CreateEscalationPolicyCommand(
						7L,
						IncidentPriority.SEV1,
						Duration.ofMinutes(10),
						Duration.ofMinutes(45))))
				.isInstanceOf(DuplicateEscalationPolicyException.class);

		verify(policies, never()).save(any(EscalationPolicy.class));
	}

	@Test
	void listsPoliciesInRepositoryOrder() {
		when(policies.findAll()).thenReturn(List.of(policy()));

		assertThat(listPolicies.execute()).containsExactly(view());
	}

	@Test
	void updatesPolicyPairAndDeadlines() {
		when(policies.findById(42L)).thenReturn(Optional.of(policy()));
		ManagedServiceView checkout = new ManagedServiceView(
				8L,
				"Checkout API",
				"Coordinates checkout.",
				Criticality.HIGH,
				new ManagedServiceView.TeamView(9L, "Checkout"));
		when(findManagedService.execute(8L)).thenReturn(Optional.of(checkout));
		when(policies.findByManagedServiceIdAndPriority(
				8L,
				IncidentPriority.SEV2)).thenReturn(Optional.empty());
		when(policies.save(any(EscalationPolicy.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		EscalationPolicyView updated = updatePolicy.execute(
				new UpdateEscalationPolicyCommand(
						42L,
						8L,
						IncidentPriority.SEV2,
						Duration.ofMinutes(20),
						Duration.ofMinutes(120)));

		assertThat(updated).isEqualTo(new EscalationPolicyView(
				42L,
				new EscalationPolicyView.ManagedServiceView(8L, "Checkout API"),
				IncidentPriority.SEV2,
				20,
				120));
	}

	@Test
	void allowsUpdateToKeepItsOwnServicePriorityPair() {
		EscalationPolicy existing = policy();
		when(policies.findById(42L)).thenReturn(Optional.of(existing));
		when(findManagedService.execute(7L)).thenReturn(Optional.of(serviceView()));
		when(policies.findByManagedServiceIdAndPriority(
				7L,
				IncidentPriority.SEV1)).thenReturn(Optional.of(existing));
		when(policies.save(any(EscalationPolicy.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		EscalationPolicyView updated = updatePolicy.execute(
				new UpdateEscalationPolicyCommand(
						42L,
						7L,
						IncidentPriority.SEV1,
						Duration.ofMinutes(15),
						Duration.ofMinutes(60)));

		assertThat(updated.acknowledgementMinutes()).isEqualTo(15);
		assertThat(updated.resolutionMinutes()).isEqualTo(60);
	}

	@Test
	void reportsMissingPolicyDuringUpdateAndDelete() {
		when(policies.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> updatePolicy.execute(
				new UpdateEscalationPolicyCommand(
						404L,
						7L,
						IncidentPriority.SEV1,
						Duration.ofMinutes(10),
						Duration.ofMinutes(45))))
				.isInstanceOf(EscalationPolicyNotFoundException.class);
		assertThatThrownBy(() -> deletePolicy.execute(404L))
				.isInstanceOf(EscalationPolicyNotFoundException.class);
	}

	@Test
	void deletesExistingPolicy() {
		EscalationPolicy existing = policy();
		when(policies.findById(42L)).thenReturn(Optional.of(existing));

		deletePolicy.execute(42L);

		verify(policies).delete(existing);
	}

	private ManagedServiceView serviceView() {
		return new ManagedServiceView(
				7L,
				"Payments API",
				"Processes card payments.",
				Criticality.CRITICAL,
				new ManagedServiceView.TeamView(3L, "Payments"));
	}

	private EscalationPolicy policy() {
		return new EscalationPolicy(
				42L,
				new PolicyManagedService(7L, "Payments API"),
				IncidentPriority.SEV1,
				Duration.ofMinutes(10),
				Duration.ofMinutes(45));
	}

	private EscalationPolicyView view() {
		return EscalationPolicyView.from(policy());
	}
}
