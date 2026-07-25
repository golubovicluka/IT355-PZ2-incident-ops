package com.golubovicluka.incident_ops.identity.domain;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {

	UserAccount save(UserAccount userAccount);

	List<UserAccount> findAll();

	Optional<UserAccount> findById(long id);

	Optional<UserAccount> findByUsername(String username);

	long count();
}
