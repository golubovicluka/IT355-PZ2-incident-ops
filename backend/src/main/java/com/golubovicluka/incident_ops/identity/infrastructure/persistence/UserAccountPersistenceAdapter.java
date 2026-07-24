package com.golubovicluka.incident_ops.identity.infrastructure.persistence;

import java.util.Optional;

import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountPersistenceAdapter implements UserAccountRepository {

	private final SpringDataUserAccountRepository users;
	private final SpringDataTeamRepository teams;
	private final UserAccountPersistenceMapper mapper = new UserAccountPersistenceMapper();

	public UserAccountPersistenceAdapter(
			SpringDataUserAccountRepository users,
			SpringDataTeamRepository teams) {
		this.users = users;
		this.teams = teams;
	}

	@Override
	public UserAccount save(UserAccount account) {
		Long teamId = account.team().id();
		if (teamId == null) {
			throw new IllegalArgumentException("user account team must be persisted first");
		}
		TeamJpaEntity team = teams.findById(teamId)
				.orElseThrow(() -> new IllegalArgumentException("team does not exist: " + teamId));
		return mapper.toDomain(users.saveAndFlush(mapper.toJpaEntity(account, team)));
	}

	@Override
	public Optional<UserAccount> findByUsername(String username) {
		return users.findByUsername(username).map(mapper::toDomain);
	}

	@Override
	public long count() {
		return users.count();
	}
}
