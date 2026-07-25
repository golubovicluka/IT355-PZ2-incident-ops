package com.golubovicluka.incident_ops.analytics.application.dto;

public record OperationalSummaryView(
		long open,
		long active,
		long resolved,
		long breached) {
}
