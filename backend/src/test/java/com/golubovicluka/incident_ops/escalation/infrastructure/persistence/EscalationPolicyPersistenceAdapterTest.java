package com.golubovicluka.incident_ops.escalation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import com.golubovicluka.incident_ops.escalation.domain.DuplicateEscalationPolicyException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicy;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyRepository;
import com.golubovicluka.incident_ops.escalation.domain.PolicyManagedService;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.integration.PostgreSQLContainerSupport;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceInUseException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import com.golubovicluka.incident_ops.servicecatalog.domain.OwningTeam;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EscalationPolicyPersistenceAdapterTest extends PostgreSQLContainerSupport {

	@Autowired
	private TeamRepository teams;

	@Autowired
	private ManagedServiceRepository services;

	@Autowired
	private EscalationPolicyRepository policies;

	@AfterEach
	void cleanDatabase() {
		policies.findAll().forEach(policies::delete);
		services.findAll().forEach(services::delete);
		teams.findAll().forEach(teams::delete);
	}

	@Test
	void persistsUniqueServicePriorityRelationAndListsDeterministically() {
		ManagedService payments = saveService("Payments API");
		ManagedService checkout = saveService("Checkout API");
		EscalationPolicy paymentsSev1 = policies.save(policy(
				payments,
				IncidentPriority.SEV1,
				10,
				45));
		policies.save(policy(checkout, IncidentPriority.SEV2, 30, 120));
		policies.save(policy(checkout, IncidentPriority.SEV1, 15, 60));

		assertThat(policies.findById(paymentsSev1.id())).contains(paymentsSev1);
		assertThat(policies.findByManagedServiceIdAndPriority(
				payments.id(),
				IncidentPriority.SEV1)).contains(paymentsSev1);
		assertThat(policies.findAll())
				.extracting(policy ->
						policy.managedService().name() + ":" + policy.priority())
				.containsExactly(
						"Checkout API:SEV1",
						"Checkout API:SEV2",
						"Payments API:SEV1");

		assertThatThrownBy(() -> services.delete(payments))
				.isInstanceOf(ManagedServiceInUseException.class);
	}

	@Test
	void databaseConstraintRejectsDuplicateServicePriorityPair() {
		ManagedService service = saveService("Customer API");
		policies.save(policy(service, IncidentPriority.SEV3, 60, 240));

		assertThatThrownBy(() ->
				policies.save(policy(service, IncidentPriority.SEV3, 45, 180)))
				.isInstanceOf(DuplicateEscalationPolicyException.class);
	}

	private ManagedService saveService(String name) {
		Team team = teams.save(Team.create(name + " Owners"));
		return services.save(ManagedService.create(
				name,
				"Owns " + name + ".",
				Criticality.HIGH,
				new OwningTeam(team.id(), team.name())));
	}

	private EscalationPolicy policy(
			ManagedService service,
			IncidentPriority priority,
			long acknowledgementMinutes,
			long resolutionMinutes) {
		return EscalationPolicy.create(
				new PolicyManagedService(service.id(), service.name()),
				priority,
				Duration.ofMinutes(acknowledgementMinutes),
				Duration.ofMinutes(resolutionMinutes));
	}
}
