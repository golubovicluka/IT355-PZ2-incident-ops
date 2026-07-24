package com.golubovicluka.incident_ops.authentication.web;

import java.time.Clock;
import java.time.Instant;

import com.golubovicluka.incident_ops.authentication.application.InvalidCredentialsException;
import com.golubovicluka.incident_ops.shared.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = LoginController.class)
public class AuthenticationExceptionHandler {

	private final Clock clock;

	public AuthenticationExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
			InvalidCredentialsException exception,
			HttpServletRequest request) {
		HttpStatus status = HttpStatus.UNAUTHORIZED;
		ApiErrorResponse error = ApiErrorResponse.of(
				Instant.now(clock),
				status.value(),
				status.getReasonPhrase(),
				"Invalid username or password",
				request.getRequestURI());
		return ResponseEntity.status(status).body(error);
	}
}
