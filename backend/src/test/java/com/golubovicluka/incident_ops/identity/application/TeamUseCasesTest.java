package com.golubovicluka.incident_ops.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.identity.application.command.CreateTeamCommand;
import com.golubovicluka.incident_ops.identity.application.command.UpdateTeamCommand;
import com.golubovicluka.incident_ops.identity.application.dto.TeamView;
import com.golubovicluka.incident_ops.identity.domain.DuplicateTeamNameException;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamInUseException;
import com.golubovicluka.incident_ops.identity.domain.TeamNotFoundException;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamUseCasesTest {

	@Mock
	private TeamRepository teams;

	private ListTeams listTeams;
	private CreateTeam createTeam;
	private UpdateTeam updateTeam;
	private DeleteTeam deleteTeam;

	@BeforeEach
	void setUp() {
		listTeams = new ListTeams(teams);
		createTeam = new CreateTeam(teams);
		updateTeam = new UpdateTeam(teams);
		deleteTeam = new DeleteTeam(teams);
	}

	@Test
	void createsTeamWithAUniqueNormalizedName() {
		when(teams.findByName("Platform Operations")).thenReturn(Optional.empty());
		when(teams.save(any(Team.class))).thenReturn(new Team(42L, "Platform Operations"));

		TeamView created = createTeam.execute(
				new CreateTeamCommand("  Platform Operations  "));

		assertThat(created).isEqualTo(new TeamView(42L, "Platform Operations"));
	}

	@Test
	void listsTeamsInRepositoryOrder() {
		when(teams.findAll()).thenReturn(List.of(
				new Team(1L, "Administration"),
				new Team(2L, "Incident Response")));

		assertThat(listTeams.execute()).containsExactly(
				new TeamView(1L, "Administration"),
				new TeamView(2L, "Incident Response"));
	}

	@Test
	void rejectsCreationWhenTheNormalizedNameAlreadyExists() {
		when(teams.findByName("Platform Operations"))
				.thenReturn(Optional.of(new Team(1L, "Platform Operations")));

		assertThatThrownBy(() -> createTeam.execute(
				new CreateTeamCommand(" Platform Operations ")))
				.isInstanceOf(DuplicateTeamNameException.class);

		verify(teams, never()).save(any(Team.class));
	}

	@Test
	void updatesAnExistingTeamName() {
		Team existing = new Team(42L, "Platform Operations");
		when(teams.findById(42L)).thenReturn(Optional.of(existing));
		when(teams.findByName("Core Platform")).thenReturn(Optional.empty());
		when(teams.save(any(Team.class))).thenReturn(new Team(42L, "Core Platform"));

		TeamView updated = updateTeam.execute(
				new UpdateTeamCommand(42L, "  Core Platform  "));

		assertThat(updated).isEqualTo(new TeamView(42L, "Core Platform"));
	}

	@Test
	void rejectsUpdateWhenAnotherTeamUsesTheName() {
		when(teams.findById(42L))
				.thenReturn(java.util.Optional.of(new Team(42L, "Platform Operations")));
		when(teams.findByName("Incident Response"))
				.thenReturn(java.util.Optional.of(new Team(7L, "Incident Response")));

		assertThatThrownBy(() -> updateTeam.execute(
				new UpdateTeamCommand(42L, "Incident Response")))
				.isInstanceOf(DuplicateTeamNameException.class);

		verify(teams, never()).save(any(Team.class));
	}

	@Test
	void allowsUpdateWhenTheNameStillBelongsToTheSameTeam() {
		Team existing = new Team(42L, "Platform Operations");
		when(teams.findById(42L)).thenReturn(Optional.of(existing));
		when(teams.findByName("Platform Operations")).thenReturn(Optional.of(existing));
		when(teams.save(existing)).thenReturn(existing);

		assertThat(updateTeam.execute(
				new UpdateTeamCommand(42L, "Platform Operations")))
				.isEqualTo(new TeamView(42L, "Platform Operations"));
	}

	@Test
	void reportsMissingTeamDuringUpdate() {
		when(teams.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> updateTeam.execute(
				new UpdateTeamCommand(404L, "Missing")))
				.isInstanceOf(TeamNotFoundException.class);
	}

	@Test
	void rejectsDeletionWhenAUserStillReferencesTheTeam() {
		Team existing = new Team(42L, "Incident Response");
		when(teams.findById(42L)).thenReturn(Optional.of(existing));
		when(teams.isReferencedByUserAccount(42L)).thenReturn(true);

		assertThatThrownBy(() -> deleteTeam.execute(42L))
				.isInstanceOf(TeamInUseException.class)
				.hasMessage("Team cannot be deleted while users or services reference it");

		verify(teams, never()).delete(any(Team.class));
	}

	@Test
	void deletesAnUnreferencedTeam() {
		Team existing = new Team(42L, "Temporary Team");
		when(teams.findById(42L)).thenReturn(Optional.of(existing));
		when(teams.isReferencedByUserAccount(42L)).thenReturn(false);

		deleteTeam.execute(42L);

		verify(teams).delete(existing);
	}

	@Test
	void reportsMissingTeamDuringDelete() {
		when(teams.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> deleteTeam.execute(404L))
				.isInstanceOf(TeamNotFoundException.class);
	}
}
