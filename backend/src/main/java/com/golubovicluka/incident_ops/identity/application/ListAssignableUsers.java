package com.golubovicluka.incident_ops.identity.application;

import java.util.List;

import com.golubovicluka.incident_ops.identity.application.dto.AssignableUserView;
import com.golubovicluka.incident_ops.identity.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListAssignableUsers {

	private final UserAccountRepository users;

	public ListAssignableUsers(UserAccountRepository users) {
		this.users = users;
	}

	@Transactional(readOnly = true)
	public List<AssignableUserView> execute() {
		return users.findAll().stream().map(AssignableUserView::from).toList();
	}
}
