package com.golubovicluka.incident_ops.identity.application;

import java.util.Optional;

import com.golubovicluka.incident_ops.identity.application.dto.AssignableUserView;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindAssignableUser {

	private final UserAccountRepository users;

	public FindAssignableUser(UserAccountRepository users) {
		this.users = users;
	}

	@Transactional(readOnly = true)
	public Optional<AssignableUserView> byId(long id) {
		return users.findById(id).map(AssignableUserView::from);
	}

	@Transactional(readOnly = true)
	public Optional<AssignableUserView> byUsername(String username) {
		return users.findByUsername(username).map(AssignableUserView::from);
	}
}
