package com.golubovicluka.incident_ops.incident.application;

import java.time.Instant;

import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentNotFoundException;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;
import org.springframework.stereotype.Service;

@Service
public class RecordIncidentEscalation {

	private final IncidentRepository incidents;

	public RecordIncidentEscalation(IncidentRepository incidents) {
		this.incidents = incidents;
	}

	public IncidentDetailView execute(
			long incidentId,
			int level,
			String reason,
			IncidentUser actor,
			Instant escalatedAt) {
		Incident incident = incidents.findById(incidentId)
				.orElseThrow(IncidentNotFoundException::new);
		Incident escalated = incident.addEscalation(
				level,
				reason,
				actor,
				escalatedAt);
		return IncidentDetailView.from(incidents.save(escalated));
	}
}
