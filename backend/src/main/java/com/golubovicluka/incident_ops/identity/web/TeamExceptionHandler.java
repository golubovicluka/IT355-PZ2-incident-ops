package com.golubovicluka.incident_ops.identity.web;

import java.time.Clock;
import java.time.Instant;

import com.golubovicluka.incident_ops.identity.domain.DuplicateTeamNameException;
import com.golubovicluka.incident_ops.identity.domain.TeamInUseException;
import com.golubovicluka.incident_ops.identity.domain.TeamNotFoundException;
import com.golubovicluka.incident_ops.shared.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TeamController.class)
public class TeamExceptionHandler {

	private final Clock clock;

	public TeamExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(TeamNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNotFound(
			TeamNotFoundException exception,
			HttpServletRequest request) {
		return response(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler({DuplicateTeamNameException.class, TeamInUseException.class})
	ResponseEntity<ApiErrorResponse> handleConflict(
			RuntimeException exception,
			HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
	}

	private ResponseEntity<ApiErrorResponse> response(
			HttpStatus status,
			String message,
			String path) {
		ApiErrorResponse error = ApiErrorResponse.of(
				Instant.now(clock),
				status.value(),
				status.getReasonPhrase(),
				message,
				path);
		return ResponseEntity.status(status).body(error);
	}
}
