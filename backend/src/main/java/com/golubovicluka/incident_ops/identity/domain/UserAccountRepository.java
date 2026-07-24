package com.golubovicluka.incident_ops.identity.domain;

import java.util.Optional;

public interface UserAccountRepository {

	UserAccount save(UserAccount userAccount);

	Optional<UserAccount> findByUsername(String username);

	long count();
}
