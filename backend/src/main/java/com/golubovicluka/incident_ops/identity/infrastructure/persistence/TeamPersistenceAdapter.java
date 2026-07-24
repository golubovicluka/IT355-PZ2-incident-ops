package com.golubovicluka.incident_ops.identity.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.identity.domain.DuplicateTeamNameException;
import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamInUseException;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class TeamPersistenceAdapter implements TeamRepository {

	private final SpringDataTeamRepository repository;
	private final SpringDataUserAccountRepository userAccounts;
	private final TeamPersistenceMapper mapper = new TeamPersistenceMapper();

	public TeamPersistenceAdapter(
			SpringDataTeamRepository repository,
			SpringDataUserAccountRepository userAccounts) {
		this.repository = repository;
		this.userAccounts = userAccounts;
	}

	@Override
	public Team save(Team team) {
		try {
			return mapper.toDomain(repository.saveAndFlush(mapper.toJpaEntity(team)));
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateTeamNameException(exception);
		}
	}

	@Override
	public List<Team> findAll() {
		return repository.findAllByOrderByNameAsc().stream().map(mapper::toDomain).toList();
	}

	@Override
	public Optional<Team> findById(long id) {
		return repository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<Team> findByName(String name) {
		return repository.findByName(name).map(mapper::toDomain);
	}

	@Override
	public boolean isReferencedByUserAccount(long teamId) {
		return userAccounts.existsByTeam_Id(teamId);
	}

	@Override
	public void delete(Team team) {
		try {
			repository.deleteById(team.id());
			repository.flush();
		} catch (DataIntegrityViolationException exception) {
			throw new TeamInUseException(exception);
		}
	}

	@Override
	public long count() {
		return repository.count();
	}
}
