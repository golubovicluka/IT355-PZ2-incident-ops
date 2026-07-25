package com.golubovicluka.incident_ops.incident.application;

import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.IncidentNotFoundException;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetIncident {

	private final IncidentRepository incidents;
	private final IncidentViewAssembler views;

	public GetIncident(
			IncidentRepository incidents,
			IncidentViewAssembler views) {
		this.incidents = incidents;
		this.views = views;
	}

	@Transactional(readOnly = true)
	public IncidentDetailView execute(long id) {
		return incidents.findById(id)
				.map(views::detail)
				.orElseThrow(IncidentNotFoundException::new);
	}
}
