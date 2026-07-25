package com.golubovicluka.incident_ops.analytics.web;

import com.golubovicluka.incident_ops.analytics.application.GetOperationalSummary;
import com.golubovicluka.incident_ops.analytics.application.dto.OperationalSummaryView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

	private final GetOperationalSummary getOperationalSummary;

	public AnalyticsController(
			GetOperationalSummary getOperationalSummary) {
		this.getOperationalSummary = getOperationalSummary;
	}

	@GetMapping("/summary")
	OperationalSummaryView summary() {
		return getOperationalSummary.execute();
	}
}
