package com.golubovicluka.incident_ops.identity.domain;

import java.util.List;
import java.util.Optional;

public interface TeamRepository {

	Team save(Team team);

	List<Team> findAll();

	Optional<Team> findById(long id);

	Optional<Team> findByName(String name);

	boolean isReferencedByUserAccount(long teamId);

	void delete(Team team);

	long count();
}
