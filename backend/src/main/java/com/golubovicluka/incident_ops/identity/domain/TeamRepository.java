package com.golubovicluka.incident_ops.identity.domain;

import java.util.Optional;

public interface TeamRepository {

	Team save(Team team);

	Optional<Team> findByName(String name);

	long count();
}
