package com.golubovicluka.incident_ops.escalation.application;

import com.golubovicluka.incident_ops.escalation.domain.EscalationRepository;
import com.golubovicluka.incident_ops.incident.application.port.IncidentDeletionCleanup;
import org.springframework.stereotype.Component;

@Component
public class DeleteIncidentEscalations implements IncidentDeletionCleanup {

	private final EscalationRepository escalations;

	public DeleteIncidentEscalations(EscalationRepository escalations) {
		this.escalations = escalations;
	}

	@Override
	public void deleteForIncident(long incidentId) {
		escalations.deleteByIncidentId(incidentId);
	}
}
