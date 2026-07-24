package com.golubovicluka.incident_ops.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import com.golubovicluka.incident_ops.integration.PostgreSQLContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.main.web-application-type=none"
})
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocalIdentityDataInitializerTest extends PostgreSQLContainerSupport {

	@Autowired
	private LocalIdentityDataInitializer initializer;

	@Autowired
	private TeamRepository teams;

	@Autowired
	private UserAccountRepository users;

	@Test
	void repeatedStartupDoesNotDuplicateTeamsOrUsersAndStoresOnlyBcryptHashes() throws Exception {
		initializer.run(new DefaultApplicationArguments());
		initializer.run(new DefaultApplicationArguments());

		assertThat(teams.count()).isEqualTo(2);
		assertThat(users.count()).isEqualTo(2);

		UserAccount responder = users.findByUsername("responder").orElseThrow();
		UserAccount administrator = users.findByUsername("admin").orElseThrow();
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		assertThat(responder.roles()).containsExactly(Role.RESPONDER);
		assertThat(administrator.roles()).containsExactly(Role.ADMIN);
		assertThat(encoder.matches("responder-demo-password", responder.passwordHash())).isTrue();
		assertThat(encoder.matches("admin-demo-password", administrator.passwordHash())).isTrue();
		assertThat(responder.passwordHash()).isNotEqualTo("responder-demo-password");
		assertThat(administrator.passwordHash()).isNotEqualTo("admin-demo-password");
	}

	@Test
	void renamedDemoTeamIsNotRecreatedAtStartup() throws Exception {
		UserAccount administrator = users.findByUsername("admin").orElseThrow();
		teams.save(administrator.team().rename("Operations Administration"));

		initializer.run(new DefaultApplicationArguments());

		assertThat(teams.count()).isEqualTo(2);
		assertThat(users.findByUsername("admin").orElseThrow().team().name())
				.isEqualTo("Operations Administration");
	}
}
