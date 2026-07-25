package com.golubovicluka.incident_ops.identity.web;

import com.golubovicluka.incident_ops.identity.application.RegisterUserAccount;
import com.golubovicluka.incident_ops.identity.application.UserAccountView;
import com.golubovicluka.incident_ops.identity.application.command.RegisterUserAccountCommand;
import com.golubovicluka.incident_ops.identity.web.request.RegistrationRequest;
import com.golubovicluka.incident_ops.identity.web.response.UserAccountResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegistrationController {

	private final RegisterUserAccount registerUserAccount;

	public RegistrationController(RegisterUserAccount registerUserAccount) {
		this.registerUserAccount = registerUserAccount;
	}

	@PostMapping("/register")
	ResponseEntity<UserAccountResponse> register(
			@Valid @RequestBody RegistrationRequest request) {
		UserAccountView registered = registerUserAccount.execute(
				new RegisterUserAccountCommand(
						request.username(),
						request.displayName(),
						request.password()));
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(UserAccountResponse.from(registered));
	}
}
