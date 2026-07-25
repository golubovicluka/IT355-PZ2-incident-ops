package com.golubovicluka.incident_ops.escalation.domain;

public interface EscalationRepository {

	Escalation save(Escalation escalation);

	int findHighestLevel(long incidentId);
}
