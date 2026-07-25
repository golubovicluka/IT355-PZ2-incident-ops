package com.golubovicluka.incident_ops.incident.application;

import com.golubovicluka.incident_ops.analytics.application.IncidentSlaProvider;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentSlaView;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentSummaryView;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import org.springframework.stereotype.Service;

@Service
public class IncidentViewAssembler {

	private final IncidentSlaProvider slaProvider;

	public IncidentViewAssembler(IncidentSlaProvider slaProvider) {
		this.slaProvider = slaProvider;
	}

	public IncidentDetailView detail(Incident incident) {
		return IncidentDetailView.from(
				incident,
				IncidentSlaView.from(slaProvider.evaluate(incident)));
	}

	public IncidentSummaryView summary(Incident incident) {
		return IncidentSummaryView.from(
				incident,
				IncidentSlaView.from(slaProvider.evaluate(incident)));
	}
}
