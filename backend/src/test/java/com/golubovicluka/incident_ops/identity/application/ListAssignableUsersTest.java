package com.golubovicluka.incident_ops.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.golubovicluka.incident_ops.identity.application.dto.AssignableUserView;
import com.golubovicluka.incident_ops.identity.domain.Role;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListAssignableUsersTest {

	@Mock
	private UserAccountRepository users;

	@Test
	void returnsPasswordFreeAssignmentChoicesInRepositoryOrder() {
		Team team = new Team(7L, "Incident Response");
		when(users.findAll()).thenReturn(List.of(
				new UserAccount(
						11L,
						"ana",
						"Ana Anić",
						"$2a$10$secret",
						Set.of(Role.RESPONDER),
						team)));

		assertThat(new ListAssignableUsers(users).execute()).containsExactly(
				new AssignableUserView(
						11L,
						"ana",
						"Ana Anić",
				new AssignableUserView.TeamView(7L, "Incident Response")));
	}

	@Test
	void findsPasswordFreeAssignmentChoiceByIdOrUsername() {
		UserAccount account = new UserAccount(
				11L,
				"ana",
				"Ana Anić",
				"$2a$10$secret",
				Set.of(Role.RESPONDER),
				new Team(7L, "Incident Response"));
		when(users.findById(11L)).thenReturn(Optional.of(account));
		when(users.findByUsername("ana")).thenReturn(Optional.of(account));
		FindAssignableUser findAssignableUser = new FindAssignableUser(users);

		assertThat(findAssignableUser.byId(11L))
				.contains(AssignableUserView.from(account));
		assertThat(findAssignableUser.byUsername("ana"))
				.contains(AssignableUserView.from(account));
	}
}
