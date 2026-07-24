package com.golubovicluka.incident_ops.shared.security;

import java.io.IOException;

import com.golubovicluka.incident_ops.shared.web.ApiErrorWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ApiErrorWriter errorWriter;

	public ApiAuthenticationEntryPoint(ApiErrorWriter errorWriter) {
		this.errorWriter = errorWriter;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authenticationException) throws IOException, ServletException {
		errorWriter.write(
				response,
				HttpStatus.UNAUTHORIZED,
				"Authentication is required or the bearer token is invalid",
				request.getRequestURI());
	}
}
