package com.golubovicluka.incident_ops.identity.web;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import com.golubovicluka.incident_ops.identity.application.RegistrationUnavailableException;
import com.golubovicluka.incident_ops.identity.domain.DuplicateUsernameException;
import com.golubovicluka.incident_ops.shared.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RegistrationController.class)
public class RegistrationExceptionHandler {

	private final Clock clock;

	public RegistrationExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(DuplicateUsernameException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateUsername(
			DuplicateUsernameException exception,
			HttpServletRequest request) {
		HttpStatus status = HttpStatus.CONFLICT;
		ApiErrorResponse error = new ApiErrorResponse(
				Instant.now(clock),
				status.value(),
				status.getReasonPhrase(),
				exception.getMessage(),
				request.getRequestURI(),
				Map.of("username", exception.getMessage()));
		return ResponseEntity.status(status).body(error);
	}

	@ExceptionHandler(RegistrationUnavailableException.class)
	ResponseEntity<ApiErrorResponse> handleRegistrationUnavailable(
			RegistrationUnavailableException exception,
			HttpServletRequest request) {
		HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
		ApiErrorResponse error = new ApiErrorResponse(
				Instant.now(clock),
				status.value(),
				status.getReasonPhrase(),
				exception.getMessage(),
				request.getRequestURI(),
				Map.of());
		return ResponseEntity.status(status).body(error);
	}
}
