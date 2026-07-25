package com.golubovicluka.incident_ops.incident.web;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import com.golubovicluka.incident_ops.incident.application.IncidentAssigneeNotFoundException;
import com.golubovicluka.incident_ops.incident.application.IncidentManagedServiceNotFoundException;
import com.golubovicluka.incident_ops.incident.application.IncidentReporterNotFoundException;
import com.golubovicluka.incident_ops.incident.domain.DuplicateIncidentReferenceCodeException;
import com.golubovicluka.incident_ops.incident.domain.IncidentNotFoundException;
import com.golubovicluka.incident_ops.shared.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = IncidentController.class)
public class IncidentExceptionHandler {

	private final Clock clock;

	public IncidentExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(IncidentNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNotFound(
			IncidentNotFoundException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.NOT_FOUND,
				exception.getMessage(),
				request.getRequestURI(),
				Map.of());
	}

	@ExceptionHandler(IncidentManagedServiceNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleMissingService(
			IncidentManagedServiceNotFoundException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				request.getRequestURI(),
				Map.of("managedServiceId", exception.getMessage()));
	}

	@ExceptionHandler(IncidentAssigneeNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleMissingAssignee(
			IncidentAssigneeNotFoundException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				request.getRequestURI(),
				Map.of("assigneeId", exception.getMessage()));
	}

	@ExceptionHandler(IncidentReporterNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleMissingReporter(
			IncidentReporterNotFoundException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.FORBIDDEN,
				exception.getMessage(),
				request.getRequestURI(),
				Map.of());
	}

	@ExceptionHandler(DuplicateIncidentReferenceCodeException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateReference(
			DuplicateIncidentReferenceCodeException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.CONFLICT,
				exception.getMessage(),
				request.getRequestURI(),
				Map.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiErrorResponse> handleUnsupportedFilterValue(
			MethodArgumentTypeMismatchException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"Request parameter is invalid",
				request.getRequestURI(),
				Map.of(exception.getName(), "Unsupported value"));
	}

	@ExceptionHandler(InvalidIncidentFilterException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidFilter(
			InvalidIncidentFilterException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"Request parameter is invalid",
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
