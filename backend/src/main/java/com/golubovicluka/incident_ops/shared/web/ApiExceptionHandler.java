package com.golubovicluka.incident_ops.shared.web;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private final Clock clock;

	public ApiExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		Map<String, String> fieldErrors = new TreeMap<>();
		exception.getBindingResult().getFieldErrors().forEach(error ->
				fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return response(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				request.getRequestURI(),
				fieldErrors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
			HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"Request body is missing or malformed",
				request.getRequestURI(),
				Map.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiErrorResponse> handleTypeMismatch(
			MethodArgumentTypeMismatchException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"Request parameter is invalid",
				request.getRequestURI(),
				Map.of(exception.getName(), "Unsupported value"));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ApiErrorResponse> handleMissingParameter(
			MissingServletRequestParameterException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"Request parameter is missing",
				request.getRequestURI(),
				Map.of(exception.getParameterName(), "Required parameter is missing"));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiErrorResponse> handleMissingResource(
			NoResourceFoundException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.NOT_FOUND,
				"Resource not found",
				request.getRequestURI(),
				Map.of());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiErrorResponse> handleUnsupportedMethod(
			HttpRequestMethodNotSupportedException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.METHOD_NOT_ALLOWED,
				"Request method is not supported",
				request.getRequestURI(),
				Map.of());
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
			HttpMediaTypeNotSupportedException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"Content type is not supported",
				request.getRequestURI(),
				Map.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpectedException(
			Exception exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred",
				request.getRequestURI(),
				Map.of());
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
