package com.golubovicluka.incident_ops.incident.application;

import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.IncidentNotFoundException;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetIncident {

	private final IncidentRepository incidents;

	public GetIncident(IncidentRepository incidents) {
		this.incidents = incidents;
	}

	@Transactional(readOnly = true)
	public IncidentDetailView execute(long id) {
		return incidents.findById(id)
				.map(IncidentDetailView::from)
				.orElseThrow(IncidentNotFoundException::new);
	}
}
