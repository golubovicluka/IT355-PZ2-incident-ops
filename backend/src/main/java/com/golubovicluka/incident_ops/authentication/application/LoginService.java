package com.golubovicluka.incident_ops.authentication.application;

import com.golubovicluka.incident_ops.identity.application.AuthenticateUserAccount;
import org.springframework.stereotype.Service;

@Service
class LoginService implements Login {

	private final AuthenticateUserAccount authenticateUserAccount;
	private final SessionTokenIssuer tokenIssuer;

	LoginService(
			AuthenticateUserAccount authenticateUserAccount,
			SessionTokenIssuer tokenIssuer) {
		this.authenticateUserAccount = authenticateUserAccount;
		this.tokenIssuer = tokenIssuer;
	}

	@Override
	public AuthenticatedSession execute(String username, String password) {
		return authenticateUserAccount.authenticate(username, password)
				.map(tokenIssuer::issue)
				.orElseThrow(InvalidCredentialsException::new);
	}
}
