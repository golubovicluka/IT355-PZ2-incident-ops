package com.golubovicluka.incident_ops.identity.application.command;

public record RegisterUserAccountCommand(
		String username,
		String displayName,
		String password) {
}
