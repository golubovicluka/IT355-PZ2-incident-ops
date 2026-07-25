package com.golubovicluka.incident_ops.escalation.web;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import com.golubovicluka.incident_ops.escalation.application.PolicyManagedServiceNotFoundException;
import com.golubovicluka.incident_ops.escalation.domain.DuplicateEscalationPolicyException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyInUseException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyNotFoundException;
import com.golubovicluka.incident_ops.escalation.domain.InvalidEscalationPolicyDeadlineException;
import com.golubovicluka.incident_ops.shared.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = EscalationPolicyAdminController.class)
public class EscalationPolicyExceptionHandler {

	private final Clock clock;

	public EscalationPolicyExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(EscalationPolicyNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNotFound(
			EscalationPolicyNotFoundException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.NOT_FOUND,
				exception.getMessage(),
				request.getRequestURI(),
				Map.of());
	}

	@ExceptionHandler({
			DuplicateEscalationPolicyException.class,
			EscalationPolicyInUseException.class
	})
	ResponseEntity<ApiErrorResponse> handleConflict(
			RuntimeException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.CONFLICT,
				exception.getMessage(),
				request.getRequestURI(),
				Map.of());
	}

	@ExceptionHandler(PolicyManagedServiceNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleMissingManagedService(
			PolicyManagedServiceNotFoundException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				request.getRequestURI(),
				Map.of("managedServiceId", exception.getMessage()));
	}

	@ExceptionHandler(InvalidEscalationPolicyDeadlineException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidDeadline(
			InvalidEscalationPolicyDeadlineException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				request.getRequestURI(),
				Map.of(exception.field(), exception.getMessage()));
	}

	private ResponseEntity<ApiErrorResponse> response(
			HttpStatus status,
			String message,
			String path,
			Map<String, String> fieldErrors) {
		ApiErrorResponse error = new ApiErrorResponse(
				Instant.now(clock),
				status.value(),
				status.getReasonPhrase(),
				message,
				path,
				fieldErrors);
		return ResponseEntity.status(status).body(error);
	}
}
