package com.golubovicluka.incident_ops.incident.application;

import java.util.List;

import com.golubovicluka.incident_ops.incident.application.dto.IncidentSummaryView;
import com.golubovicluka.incident_ops.incident.domain.IncidentCriteria;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListIncidents {

	private final IncidentRepository incidents;
	private final IncidentViewAssembler views;

	public ListIncidents(
			IncidentRepository incidents,
			IncidentViewAssembler views) {
		this.incidents = incidents;
		this.views = views;
	}

	@Transactional(readOnly = true)
	public List<IncidentSummaryView> execute(IncidentCriteria criteria) {
		return incidents.findAll(criteria).stream()
				.map(views::summary)
				.toList();
	}
}
