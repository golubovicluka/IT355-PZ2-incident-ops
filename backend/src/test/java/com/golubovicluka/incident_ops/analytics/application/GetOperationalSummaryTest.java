package com.golubovicluka.incident_ops.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import com.golubovicluka.incident_ops.analytics.domain.SlaEvaluation;
import com.golubovicluka.incident_ops.incident.application.ListIncidents;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentSlaView;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentSummaryView;
import com.golubovicluka.incident_ops.incident.domain.IncidentCriteria;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetOperationalSummaryTest {

	@Mock
	private ListIncidents listIncidents;

	@Test
	void countsOpenActiveResolvedAndBreachedIncidents() {
		IncidentCriteria all = new IncidentCriteria(null, null, null);
		when(listIncidents.execute(all)).thenReturn(List.of(
				summary(1L, IncidentStatus.OPEN, false),
				summary(2L, IncidentStatus.ACKNOWLEDGED, false),
				summary(3L, IncidentStatus.INVESTIGATING, true),
				summary(4L, IncidentStatus.RESOLVED, false),
				summary(5L, IncidentStatus.CLOSED, true)));

		var summary = new GetOperationalSummary(listIncidents).execute();

		assertThat(summary.open()).isEqualTo(1);
		assertThat(summary.active()).isEqualTo(2);
		assertThat(summary.resolved()).isEqualTo(2);
		assertThat(summary.breached()).isEqualTo(2);
	}

	private IncidentSummaryView summary(
			long id,
			IncidentStatus status,
			boolean breached) {
		Instant createdAt = Instant.parse("2026-07-25T08:00:00Z");
		return new IncidentSummaryView(
				id,
				"INC-" + id,
				"Incident " + id,
				IncidentPriority.SEV2,
				status,
				new IncidentSummaryView.ManagedServiceView(
						7L,
						"Payments API"),
				null,
				createdAt,
				createdAt,
				breached
						? IncidentSlaView.from(SlaEvaluation.breached(
								com.golubovicluka.incident_ops.analytics.domain.SlaPhase.RESOLUTION,
								createdAt.plusSeconds(3600)))
						: IncidentSlaView.from(SlaEvaluation.onTrack(
								com.golubovicluka.incident_ops.analytics.domain.SlaPhase.RESOLUTION,
								createdAt.plusSeconds(3600))));
	}
}
