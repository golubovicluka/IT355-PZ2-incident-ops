package com.golubovicluka.incident_ops.shared.web;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		Map<String, String> fieldErrors) {

	public ApiErrorResponse {
		fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
	}

	public static ApiErrorResponse of(
			Instant timestamp,
			int status,
			String error,
			String message,
			String path) {
		return new ApiErrorResponse(timestamp, status, error, message, path, Map.of());
	}
}
