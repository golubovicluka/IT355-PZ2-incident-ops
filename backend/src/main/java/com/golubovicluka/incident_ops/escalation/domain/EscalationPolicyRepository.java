package com.golubovicluka.incident_ops.escalation.domain;

import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;

public interface EscalationPolicyRepository {

	EscalationPolicy save(EscalationPolicy policy);

	List<EscalationPolicy> findAll();

	Optional<EscalationPolicy> findById(long id);

	Optional<EscalationPolicy> findByManagedServiceIdAndPriority(
			long managedServiceId,
			IncidentPriority priority);

	/**
	 * Deletes an unreferenced policy.
	 *
	 * @throws EscalationPolicyInUseException when persisted rule data still
	 *         references the policy
	 */
	void delete(EscalationPolicy policy);
}
