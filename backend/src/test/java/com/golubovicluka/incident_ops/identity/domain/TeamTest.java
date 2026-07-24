package com.golubovicluka.incident_ops.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class TeamTest {

	@Test
	void createsTeamWithNormalizedNameWithoutSpring() {
		Team team = Team.create("  Platform Operations  ");

		assertThat(team.id()).isNull();
		assertThat(team.name()).isEqualTo("Platform Operations");
	}

	@Test
	void rejectsBlankName() {
		assertThatIllegalArgumentException().isThrownBy(() -> Team.create("  "));
	}

	@Test
	void rejectsNameLongerThanThePersistenceLimit() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Team.create("a".repeat(101)))
				.withMessage("name must not exceed 100 characters");
	}
}
