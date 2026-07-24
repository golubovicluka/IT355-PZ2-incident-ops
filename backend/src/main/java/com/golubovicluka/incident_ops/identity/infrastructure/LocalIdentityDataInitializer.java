package com.golubovicluka.incident_ops.identity.infrastructure;

import com.golubovicluka.incident_ops.identity.application.InitializeLocalIdentityData;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalIdentityDataInitializer implements ApplicationRunner {

	private final InitializeLocalIdentityData initializeLocalIdentityData;

	public LocalIdentityDataInitializer(InitializeLocalIdentityData initializeLocalIdentityData) {
		this.initializeLocalIdentityData = initializeLocalIdentityData;
	}

	@Override
	public void run(ApplicationArguments args) {
		initializeLocalIdentityData.initialize();
	}
}
