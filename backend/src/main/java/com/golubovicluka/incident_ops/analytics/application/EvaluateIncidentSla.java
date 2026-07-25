package com.golubovicluka.incident_ops.analytics.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.golubovicluka.incident_ops.analytics.domain.IncidentSlaEvaluator;
import com.golubovicluka.incident_ops.analytics.domain.SlaEvaluation;
import com.golubovicluka.incident_ops.escalation.application.FindEscalationPolicy;
import com.golubovicluka.incident_ops.escalation.application.dto.EscalationPolicyView;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import org.springframework.stereotype.Service;

@Service
public class EvaluateIncidentSla implements IncidentSlaProvider {

	private final FindEscalationPolicy findPolicy;
	private final Clock clock;
	private final IncidentSlaEvaluator evaluator = new IncidentSlaEvaluator();

	public EvaluateIncidentSla(
			FindEscalationPolicy findPolicy,
			Clock clock) {
		this.findPolicy = findPolicy;
		this.clock = clock;
	}

	@Override
	public SlaEvaluation evaluate(Incident incident) {
		EscalationPolicyView policy = findPolicy.execute(
				incident.managedService().id(),
				incident.priority()).orElse(null);
		if (policy == null) {
			return SlaEvaluation.notConfigured();
		}
		return evaluator.evaluate(
				incident.status(),
				incident.createdAt(),
				incident.acknowledgedAt(),
				incident.resolvedAt(),
				Duration.ofMinutes(policy.acknowledgementMinutes()),
				Duration.ofMinutes(policy.resolutionMinutes()),
				Instant.now(clock));
	}
}
