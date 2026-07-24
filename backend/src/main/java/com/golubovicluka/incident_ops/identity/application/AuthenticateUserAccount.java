package com.golubovicluka.incident_ops.identity.application;

import java.util.Locale;
import java.util.Optional;

import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticateUserAccount {

	private final UserAccountRepository users;
	private final PasswordHashing passwordHashing;

	public AuthenticateUserAccount(UserAccountRepository users, PasswordHashing passwordHashing) {
		this.users = users;
		this.passwordHashing = passwordHashing;
	}

	@Transactional(readOnly = true)
	public Optional<AuthenticatedUserAccount> authenticate(String username, String plaintextPassword) {
		String normalizedUsername = username.strip().toLowerCase(Locale.ROOT);
		return users.findByUsername(normalizedUsername)
				.filter(account -> passwordHashing.matches(plaintextPassword, account.passwordHash()))
				.map(this::authenticatedUser);
	}

	private AuthenticatedUserAccount authenticatedUser(UserAccount account) {
		return new AuthenticatedUserAccount(
				account.username(),
				account.displayName(),
				account.roles());
	}
}
