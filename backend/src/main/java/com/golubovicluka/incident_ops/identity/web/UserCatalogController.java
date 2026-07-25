package com.golubovicluka.incident_ops.identity.web;

import java.util.List;

import com.golubovicluka.incident_ops.identity.application.ListAssignableUsers;
import com.golubovicluka.incident_ops.identity.web.response.AssignableUserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogs/users")
public class UserCatalogController {

	private final ListAssignableUsers listAssignableUsers;

	public UserCatalogController(ListAssignableUsers listAssignableUsers) {
		this.listAssignableUsers = listAssignableUsers;
	}

	@GetMapping
	List<AssignableUserResponse> list() {
		return listAssignableUsers.execute().stream()
				.map(AssignableUserResponse::from)
				.toList();
	}
}
