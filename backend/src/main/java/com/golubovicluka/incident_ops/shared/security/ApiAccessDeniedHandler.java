package com.golubovicluka.incident_ops.shared.security;

import java.io.IOException;

import com.golubovicluka.incident_ops.shared.web.ApiErrorWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

	private final ApiErrorWriter errorWriter;

	public ApiAccessDeniedHandler(ApiErrorWriter errorWriter) {
		this.errorWriter = errorWriter;
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		errorWriter.write(
				response,
				HttpStatus.FORBIDDEN,
				"You do not have permission to access this resource",
				request.getRequestURI());
	}
}
