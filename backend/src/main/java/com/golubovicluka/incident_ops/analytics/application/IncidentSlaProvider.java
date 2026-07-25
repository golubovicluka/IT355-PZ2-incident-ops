package com.golubovicluka.incident_ops.analytics.application;

import com.golubovicluka.incident_ops.analytics.domain.SlaEvaluation;
import com.golubovicluka.incident_ops.incident.domain.Incident;

public interface IncidentSlaProvider {

	SlaEvaluation evaluate(Incident incident);
}
