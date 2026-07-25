package com.golubovicluka.incident_ops.servicecatalog.web;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import com.golubovicluka.incident_ops.servicecatalog.application.OwningTeamNotFoundException;
import com.golubovicluka.incident_ops.servicecatalog.domain.DuplicateManagedServiceNameException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceInUseException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceNotFoundException;
import com.golubovicluka.incident_ops.shared.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ManagedServiceAdminController.class)
public class ManagedServiceExceptionHandler {

	private final Clock clock;

	public ManagedServiceExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(ManagedServiceNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNotFound(
			ManagedServiceNotFoundException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.NOT_FOUND,
				exception.getMessage(),
				request.getRequestURI(),
				Map.of());
	}

	@ExceptionHandler({
			DuplicateManagedServiceNameException.class,
			ManagedServiceInUseException.class
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

	@ExceptionHandler(OwningTeamNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleMissingOwningTeam(
			OwningTeamNotFoundException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				request.getRequestURI(),
				Map.of("owningTeamId", exception.getMessage()));
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
