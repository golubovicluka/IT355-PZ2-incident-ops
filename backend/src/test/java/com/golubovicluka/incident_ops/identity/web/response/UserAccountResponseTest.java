package com.golubovicluka.incident_ops.identity.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import com.golubovicluka.incident_ops.identity.application.UserAccountView;
import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class UserAccountResponseTest {

	@Test
	void responseContainsIdentityDataButNeverThePasswordHash() throws Exception {
		String passwordHash = "$2a$10$mustNeverReachAResponse";
		UserAccount account = new UserAccount(
				7L,
				"responder",
				"Response Engineer",
				passwordHash,
				Set.of(Role.RESPONDER),
				new Team(3L, "Incident Response"));

		UserAccountResponse response = UserAccountResponse.from(UserAccountView.from(account));
		String json = JsonMapper.builder().build().writeValueAsString(response);

		assertThat(json)
				.contains("\"username\":\"responder\"")
				.contains("\"team\"")
				.doesNotContain(passwordHash)
				.doesNotContainIgnoringCase("password");
		assertThat(UserAccountResponse.class.getRecordComponents())
				.extracting(component -> component.getName().toLowerCase())
				.noneMatch(name -> name.contains("password"));
	}
}
