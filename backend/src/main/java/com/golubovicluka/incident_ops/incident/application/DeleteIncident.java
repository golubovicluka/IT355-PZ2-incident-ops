package com.golubovicluka.incident_ops.incident.application;

import java.util.List;

import com.golubovicluka.incident_ops.incident.application.port.IncidentDeletionCleanup;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentNotFoundException;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteIncident {

	private final IncidentRepository incidents;
	private final List<IncidentDeletionCleanup> dependentRecordCleanups;

	public DeleteIncident(
			IncidentRepository incidents,
			List<IncidentDeletionCleanup> dependentRecordCleanups) {
		this.incidents = incidents;
		this.dependentRecordCleanups = List.copyOf(dependentRecordCleanups);
	}

	@Transactional
	public void execute(long incidentId) {
		Incident incident = incidents.findById(incidentId)
				.orElseThrow(IncidentNotFoundException::new);
		dependentRecordCleanups.forEach(
				cleanup -> cleanup.deleteForIncident(incidentId));
		incidents.delete(incident);
	}
}
