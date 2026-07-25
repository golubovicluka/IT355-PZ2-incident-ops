package com.golubovicluka.incident_ops.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import com.golubovicluka.incident_ops.identity.application.command.RegisterUserAccountCommand;
import com.golubovicluka.incident_ops.identity.domain.DuplicateUsernameException;
import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterUserAccountTest {

	@Mock
	private TeamRepository teams;

	@Mock
	private UserAccountRepository users;

	@Mock
	private PasswordHashing passwordHashing;

	private RegisterUserAccount registerUserAccount;

	@BeforeEach
	void setUp() {
		registerUserAccount = new RegisterUserAccount(teams, users, passwordHashing);
	}

	@Test
	void registersNormalizedResponderInTheRegistrationTeamWithHashedPassword() {
		Team registrationTeam = new Team(7L, RegisterUserAccount.REGISTRATION_TEAM_NAME);
		when(users.findByUsername("new.responder")).thenReturn(Optional.empty());
		when(teams.findByName(RegisterUserAccount.REGISTRATION_TEAM_NAME))
				.thenReturn(Optional.of(registrationTeam));
		when(passwordHashing.hash("strong-password")).thenReturn("$2a$10$hashedPassword");
		when(users.save(any(UserAccount.class))).thenAnswer(invocation -> {
			UserAccount account = invocation.getArgument(0);
			return new UserAccount(
					42L,
					account.username(),
					account.displayName(),
					account.passwordHash(),
					account.roles(),
					account.team());
		});

		UserAccountView registered = registerUserAccount.execute(
				new RegisterUserAccountCommand(
						" New.Responder ",
						" New Response Engineer ",
						"strong-password"));

		ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
		verify(users).save(accountCaptor.capture());
		UserAccount saved = accountCaptor.getValue();
		assertThat(saved.username()).isEqualTo("new.responder");
		assertThat(saved.displayName()).isEqualTo("New Response Engineer");
		assertThat(saved.passwordHash()).isEqualTo("$2a$10$hashedPassword");
		assertThat(saved.roles()).containsExactly(Role.RESPONDER);
		assertThat(saved.team()).isEqualTo(registrationTeam);
		assertThat(registered.id()).isEqualTo(42L);
		assertThat(registered.roles()).containsExactly("RESPONDER");
		verify(passwordHashing).hash("strong-password");
	}

	@Test
	void rejectsRegistrationWhenTheConfiguredTeamIsMissing() {
		when(users.findByUsername("new.responder")).thenReturn(Optional.empty());
		when(teams.findByName(RegisterUserAccount.REGISTRATION_TEAM_NAME))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> registerUserAccount.execute(
				new RegisterUserAccountCommand(
						"new.responder",
						"New Response Engineer",
						"strong-password")))
				.isInstanceOf(RegistrationUnavailableException.class)
				.hasMessage("Registration is temporarily unavailable");

		verify(teams, never()).save(any());
		verify(passwordHashing, never()).hash(any());
		verify(users, never()).save(any());
	}

	@Test
	void rejectsAnExistingUsernameBeforeHashingOrSaving() {
		Team team = new Team(7L, RegisterUserAccount.REGISTRATION_TEAM_NAME);
		UserAccount existing = new UserAccount(
				42L,
				"new.responder",
				"Existing Responder",
				"$2a$10$existingHash",
				Set.of(Role.RESPONDER),
				team);
		when(users.findByUsername("new.responder")).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> registerUserAccount.execute(
				new RegisterUserAccountCommand(
						"New.Responder",
						"Another Responder",
						"strong-password")))
				.isInstanceOf(DuplicateUsernameException.class)
				.hasMessage("Username is already registered");

		verify(passwordHashing, never()).hash(any());
		verify(users, never()).save(any());
	}
}
