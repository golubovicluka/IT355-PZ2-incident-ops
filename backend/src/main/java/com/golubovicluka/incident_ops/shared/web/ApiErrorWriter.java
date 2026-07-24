package com.golubovicluka.incident_ops.shared.web;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ApiErrorWriter {

	private final ObjectMapper objectMapper;
	private final Clock clock;

	public ApiErrorWriter(ObjectMapper objectMapper, Clock clock) {
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	public void write(
			HttpServletResponse response,
			HttpStatus status,
			String message,
			String path) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		ApiErrorResponse error = ApiErrorResponse.of(
				Instant.now(clock),
				status.value(),
				status.getReasonPhrase(),
				message,
				path);
		objectMapper.writeValue(response.getOutputStream(), error);
	}
}
