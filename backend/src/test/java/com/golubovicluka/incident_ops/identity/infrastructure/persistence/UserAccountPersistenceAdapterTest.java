package com.golubovicluka.incident_ops.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

import com.golubovicluka.incident_ops.identity.domain.DuplicateUsernameException;
import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserAccountPersistenceAdapterTest {

	@Mock
	private SpringDataUserAccountRepository users;

	@Mock
	private SpringDataTeamRepository teams;

	private UserAccountPersistenceAdapter adapter;
	private UserAccount account;

	@BeforeEach
	void setUp() {
		adapter = new UserAccountPersistenceAdapter(users, teams);
		Team team = new Team(7L, "Incident Response");
		account = UserAccount.create(
				"new.responder",
				"New Response Engineer",
				"$2a$10$hashedPassword",
				Set.of(Role.RESPONDER),
				team);
		when(teams.findById(7L))
				.thenReturn(Optional.of(new TeamJpaEntity(7L, "Incident Response")));
	}

	@Test
	void translatesOnlyTheUsernameUniqueConstraint() {
		DataIntegrityViolationException failure =
				integrityFailure("user_accounts_username_key");
		when(users.saveAndFlush(any())).thenThrow(failure);

		assertThatThrownBy(() -> adapter.save(account))
				.isInstanceOf(DuplicateUsernameException.class)
				.hasMessage("Username is already registered");
	}

	@Test
	void preservesUnrelatedIntegrityFailures() {
		DataIntegrityViolationException failure =
				integrityFailure("fk_user_accounts_team");
		when(users.saveAndFlush(any())).thenThrow(failure);

		assertThatThrownBy(() -> adapter.save(account))
				.isSameAs(failure);
	}

	private DataIntegrityViolationException integrityFailure(String constraintName) {
		ConstraintViolationException cause = new ConstraintViolationException(
				"constraint failed",
				new SQLException("constraint failed"),
				constraintName);
		return new DataIntegrityViolationException("persistence failed", cause);
	}
}
