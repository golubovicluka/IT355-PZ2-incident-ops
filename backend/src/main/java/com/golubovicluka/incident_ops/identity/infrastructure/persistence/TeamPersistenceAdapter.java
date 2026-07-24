package com.golubovicluka.incident_ops.identity.infrastructure.persistence;

import java.util.Optional;

import com.golubovicluka.incident_ops.identity.domain.Team;
import com.golubovicluka.incident_ops.identity.domain.TeamRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TeamPersistenceAdapter implements TeamRepository {

	private final SpringDataTeamRepository repository;
	private final TeamPersistenceMapper mapper = new TeamPersistenceMapper();

	public TeamPersistenceAdapter(SpringDataTeamRepository repository) {
		this.repository = repository;
	}

	@Override
	public Team save(Team team) {
		return mapper.toDomain(repository.saveAndFlush(mapper.toJpaEntity(team)));
	}

	@Override
	public Optional<Team> findByName(String name) {
		return repository.findByName(name).map(mapper::toDomain);
	}

	@Override
	public long count() {
		return repository.count();
	}
}
