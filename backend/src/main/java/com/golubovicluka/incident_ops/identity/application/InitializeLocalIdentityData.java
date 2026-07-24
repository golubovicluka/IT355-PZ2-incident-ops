package com.golubovicluka.incident_ops.identity.application;

import java.util.Set;

import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InitializeLocalIdentityData {

	private static final String RESPONDER_TEAM = "Incident Response";
	private static final String ADMIN_TEAM = "Administration";

	private final TeamRepository teams;
	private final UserAccountRepository users;
	private final PasswordHashing passwordHashing;

	public InitializeLocalIdentityData(
			TeamRepository teams,
			UserAccountRepository users,
			PasswordHashing passwordHashing) {
		this.teams = teams;
		this.users = users;
		this.passwordHashing = passwordHashing;
	}

	@Transactional
	public void initialize() {
		Team responderTeam = findAssignedTeamOrCreate("responder", RESPONDER_TEAM);
		Team adminTeam = findAssignedTeamOrCreate("admin", ADMIN_TEAM);

		createUserIfMissing(
				"responder",
				"Response Engineer",
				"responder-demo-password",
				Role.RESPONDER,
				responderTeam);
		createUserIfMissing(
				"admin",
				"Administrator",
				"admin-demo-password",
				Role.ADMIN,
				adminTeam);
	}

	private Team findAssignedTeamOrCreate(String username, String defaultTeamName) {
		return users.findByUsername(username)
				.map(UserAccount::team)
				.orElseGet(() -> findOrCreateTeam(defaultTeamName));
	}

	private Team findOrCreateTeam(String name) {
		return teams.findByName(name).orElseGet(() -> teams.save(Team.create(name)));
	}

	private void createUserIfMissing(
			String username,
			String displayName,
			String plaintextPassword,
			Role role,
			Team team) {
		if (users.findByUsername(username).isEmpty()) {
			users.save(UserAccount.create(
					username,
					displayName,
					passwordHashing.hash(plaintextPassword),
					Set.of(role),
					team));
		}
	}
}
