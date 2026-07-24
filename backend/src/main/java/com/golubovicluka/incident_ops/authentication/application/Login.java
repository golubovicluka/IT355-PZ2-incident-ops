package com.golubovicluka.incident_ops.authentication.application;

public interface Login {

	AuthenticatedSession execute(String username, String password);
}
