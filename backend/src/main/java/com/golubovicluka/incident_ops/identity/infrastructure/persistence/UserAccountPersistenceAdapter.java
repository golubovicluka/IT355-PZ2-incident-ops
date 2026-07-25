package com.golubovicluka.incident_ops.identity.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.identity.domain.DuplicateUsernameException;
import com.golubovicluka.incident_ops.identity.domain.UserAccount;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
		try {
			return mapper.toDomain(users.saveAndFlush(mapper.toJpaEntity(account, team)));
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateUsernameException(exception);
		}
	}

	@Override
	public List<UserAccount> findAll() {
		return users.findAllByOrderByDisplayNameAsc().stream()
				.map(mapper::toDomain)
				.toList();
	}

	@Override
	public Optional<UserAccount> findById(long id) {
		return users.findById(id).map(mapper::toDomain);
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
