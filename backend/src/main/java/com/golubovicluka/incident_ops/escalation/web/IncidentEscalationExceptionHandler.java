package com.golubovicluka.incident_ops.escalation.web;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import com.golubovicluka.incident_ops.incident.application.IncidentActorNotFoundException;
import com.golubovicluka.incident_ops.incident.domain.IncidentEscalationNotAllowedException;
import com.golubovicluka.incident_ops.incident.domain.IncidentNotFoundException;
import com.golubovicluka.incident_ops.shared.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = IncidentEscalationController.class)
public class IncidentEscalationExceptionHandler {

	private final Clock clock;

	public IncidentEscalationExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(IncidentNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNotFound(
			IncidentNotFoundException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.NOT_FOUND,
				exception.getMessage(),
				request.getRequestURI());
	}

	@ExceptionHandler(IncidentActorNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleMissingActor(
			IncidentActorNotFoundException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.FORBIDDEN,
				exception.getMessage(),
				request.getRequestURI());
	}

	@ExceptionHandler(IncidentEscalationNotAllowedException.class)
	ResponseEntity<ApiErrorResponse> handleBlockedStatus(
			IncidentEscalationNotAllowedException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.CONFLICT,
				exception.getMessage(),
				request.getRequestURI());
	}

	private ResponseEntity<ApiErrorResponse> response(
			HttpStatus status,
			String message,
			String path) {
		return ResponseEntity.status(status).body(new ApiErrorResponse(
				Instant.now(clock),
				status.value(),
				status.getReasonPhrase(),
				message,
				path,
				Map.of()));
	}
}
