package com.golubovicluka.incident_ops.identity.application;

public interface PasswordHashing {

	String hash(String plaintext);

	boolean matches(String plaintext, String passwordHash);
}
