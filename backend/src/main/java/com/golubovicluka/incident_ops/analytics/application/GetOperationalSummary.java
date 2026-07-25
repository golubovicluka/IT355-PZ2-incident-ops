package com.golubovicluka.incident_ops.analytics.application;

import java.util.List;

import com.golubovicluka.incident_ops.analytics.application.dto.OperationalSummaryView;
import com.golubovicluka.incident_ops.analytics.domain.SlaState;
import com.golubovicluka.incident_ops.incident.application.ListIncidents;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentSummaryView;
import com.golubovicluka.incident_ops.incident.domain.IncidentCriteria;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOperationalSummary {

	private static final IncidentCriteria ALL_INCIDENTS =
			new IncidentCriteria(null, null, null);

	private final ListIncidents listIncidents;

	public GetOperationalSummary(ListIncidents listIncidents) {
		this.listIncidents = listIncidents;
	}

	@Transactional(readOnly = true)
	public OperationalSummaryView execute() {
		List<IncidentSummaryView> incidents =
				listIncidents.execute(ALL_INCIDENTS);
		return new OperationalSummaryView(
				countStatuses(incidents, IncidentStatus.OPEN),
				countStatuses(
						incidents,
						IncidentStatus.ACKNOWLEDGED,
						IncidentStatus.INVESTIGATING),
				countStatuses(
						incidents,
						IncidentStatus.RESOLVED,
						IncidentStatus.CLOSED),
				incidents.stream()
						.filter(incident ->
								incident.sla().state() == SlaState.BREACHED)
						.count());
	}

	private long countStatuses(
			List<IncidentSummaryView> incidents,
			IncidentStatus... statuses) {
		List<IncidentStatus> included = List.of(statuses);
		return incidents.stream()
				.filter(incident -> included.contains(incident.status()))
				.count();
	}
}
