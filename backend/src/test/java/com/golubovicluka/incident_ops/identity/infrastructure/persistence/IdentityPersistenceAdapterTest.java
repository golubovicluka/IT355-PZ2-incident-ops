package com.golubovicluka.incident_ops.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import com.golubovicluka.incident_ops.integration.PostgreSQLContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TeamPersistenceAdapter.class, UserAccountPersistenceAdapter.class})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IdentityPersistenceAdapterTest extends PostgreSQLContainerSupport {

	@Autowired
	private TeamRepository teams;

	@Autowired
	private UserAccountRepository users;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void storesAndLoadsTeamThroughDomainRepository() {
		Team saved = teams.save(Team.create("Platform Operations"));

		assertThat(saved.id()).isNotNull();
		assertThat(teams.findByName("Platform Operations")).contains(saved);
	}

	@Test
	void storesAndLoadsUserWithTeamAndRoles() {
		Team team = teams.save(Team.create("Incident Response"));
		UserAccount saved = users.save(UserAccount.create(
				"responder",
				"Response Engineer",
				"$2a$10$persistedHash",
				Set.of(Role.RESPONDER),
				team));
		entityManager.clear();

		UserAccount loaded = users.findByUsername("responder").orElseThrow();

		assertThat(loaded.id()).isEqualTo(saved.id());
		assertThat(loaded.displayName()).isEqualTo("Response Engineer");
		assertThat(loaded.passwordHash()).isEqualTo("$2a$10$persistedHash");
		assertThat(loaded.roles()).containsExactly(Role.RESPONDER);
		assertThat(loaded.team()).isEqualTo(team);
	}

	@Test
	void enforcesUniqueTeamNames() {
		teams.save(Team.create("Platform Operations"));

		assertThatThrownBy(() -> teams.save(Team.create("Platform Operations")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void enforcesUniqueUsernames() {
		Team team = teams.save(Team.create("Incident Response"));
		users.save(UserAccount.create(
				"responder",
				"First Response Engineer",
				"$2a$10$firstHash",
				Set.of(Role.RESPONDER),
				team));

		assertThatThrownBy(() -> users.save(UserAccount.create(
				"responder",
				"Second Response Engineer",
				"$2a$10$secondHash",
				Set.of(Role.RESPONDER),
				team)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
