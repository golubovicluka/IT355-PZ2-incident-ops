package com.golubovicluka.incident_ops.identity.application;

import java.util.Locale;
import java.util.Set;

import com.golubovicluka.incident_ops.identity.application.command.RegisterUserAccountCommand;
import com.golubovicluka.incident_ops.identity.domain.DuplicateUsernameException;
import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserAccount {

	static final String REGISTRATION_TEAM_NAME = "Incident Response";

	private final TeamRepository teams;
	private final UserAccountRepository users;
	private final PasswordHashing passwordHashing;

	public RegisterUserAccount(
			TeamRepository teams,
			UserAccountRepository users,
			PasswordHashing passwordHashing) {
		this.teams = teams;
		this.users = users;
		this.passwordHashing = passwordHashing;
	}

	@Transactional
	public UserAccountView execute(RegisterUserAccountCommand command) {
		String username = command.username().strip().toLowerCase(Locale.ROOT);
		if (users.findByUsername(username).isPresent()) {
			throw new DuplicateUsernameException();
		}

		Team registrationTeam = teams.findByName(REGISTRATION_TEAM_NAME)
				.orElseGet(() -> teams.save(Team.create(REGISTRATION_TEAM_NAME)));
		UserAccount account = UserAccount.create(
				username,
				command.displayName(),
				passwordHashing.hash(command.password()),
				Set.of(Role.RESPONDER),
				registrationTeam);
		return UserAccountView.from(users.save(account));
	}
}
