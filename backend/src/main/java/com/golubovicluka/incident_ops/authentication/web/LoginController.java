package com.golubovicluka.incident_ops.authentication.web;

import com.golubovicluka.incident_ops.authentication.application.Login;
import com.golubovicluka.incident_ops.authentication.web.request.LoginRequest;
import com.golubovicluka.incident_ops.authentication.web.response.SessionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

	private final Login login;

	public LoginController(Login login) {
		this.login = login;
	}

	@PostMapping("/login")
	public SessionResponse login(@Valid @RequestBody LoginRequest request) {
		return SessionResponse.from(login.execute(request.username(), request.password()));
	}
}
