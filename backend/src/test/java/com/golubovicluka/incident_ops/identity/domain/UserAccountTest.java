package com.golubovicluka.incident_ops.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Set;

import org.junit.jupiter.api.Test;

class UserAccountTest {

	@Test
	void createsAccountWithExplicitTeamAndRoles() {
		Team team = new Team(42L, "Platform Operations");

		UserAccount account = UserAccount.create(
				"  Responder.One  ",
				"  Response Engineer  ",
				"$2a$10$sensitiveHash",
				Set.of(Role.RESPONDER),
				team);

		assertThat(account.id()).isNull();
		assertThat(account.username()).isEqualTo("responder.one");
		assertThat(account.displayName()).isEqualTo("Response Engineer");
		assertThat(account.passwordHash()).isEqualTo("$2a$10$sensitiveHash");
		assertThat(account.roles()).containsExactly(Role.RESPONDER);
		assertThat(account.team()).isEqualTo(team);
	}

	@Test
	void doesNotRevealPasswordHashInStringRepresentation() {
		String hash = "$2a$10$mustNeverAppear";
		UserAccount account = UserAccount.create(
				"admin",
				"Administrator",
				hash,
				Set.of(Role.ADMIN),
				Team.create("Administration"));

		assertThat(account.toString()).doesNotContain(hash);
	}

	@Test
	void requiresAtLeastOneRole() {
		assertThatIllegalArgumentException().isThrownBy(() -> UserAccount.create(
				"responder",
				"Response Engineer",
				"$2a$10$hash",
				Set.of(),
				Team.create("Operations")));
	}
}
