package com.golubovicluka.incident_ops.identity.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitializeLocalIdentityDataTest {

	@Mock
	private TeamRepository teams;

	@Mock
	private UserAccountRepository users;

	@Mock
	private PasswordHashing passwordHashing;

	private InitializeLocalIdentityData identityData;

	@BeforeEach
	void setUp() {
		identityData = new InitializeLocalIdentityData(teams, users, passwordHashing);
	}

	@Test
	void renamedDemoTeamsAreNotRecreatedOnLaterStartup() {
		when(users.findByUsername("responder")).thenReturn(Optional.of(account(
				"responder",
				"Response Engineer",
				Role.RESPONDER,
				new Team(1L, "Core Response"))));
		when(users.findByUsername("admin")).thenReturn(Optional.of(account(
				"admin",
				"Administrator",
				Role.ADMIN,
				new Team(2L, "Operations Administration"))));

		identityData.initialize();

		verify(teams, never()).save(any(Team.class));
	}

	private UserAccount account(
			String username,
			String displayName,
			Role role,
			Team team) {
		return new UserAccount(
				1L,
				username,
				displayName,
				"$2a$10$existingHash",
				Set.of(role),
				team);
	}
}
