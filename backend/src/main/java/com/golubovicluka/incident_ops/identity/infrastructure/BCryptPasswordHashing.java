package com.golubovicluka.incident_ops.identity.infrastructure;

import com.golubovicluka.incident_ops.identity.application.PasswordHashing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordHashing implements PasswordHashing {

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@Override
	public String hash(String plaintext) {
		return encoder.encode(plaintext);
	}

	@Override
	public boolean matches(String plaintext, String passwordHash) {
		return encoder.matches(plaintext, passwordHash);
	}
}
