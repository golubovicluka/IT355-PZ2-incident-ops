package com.golubovicluka.incident_ops.authentication.application;

import com.golubovicluka.incident_ops.identity.application.AuthenticatedUserAccount;

public interface SessionTokenIssuer {

	AuthenticatedSession issue(AuthenticatedUserAccount userAccount);
}
