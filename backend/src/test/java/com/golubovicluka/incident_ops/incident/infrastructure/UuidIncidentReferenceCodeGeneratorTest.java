package com.golubovicluka.incident_ops.incident.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UuidIncidentReferenceCodeGeneratorTest {

	@Test
	void createsCompactIncidentReferenceCodes() {
		UuidIncidentReferenceCodeGenerator generator =
				new UuidIncidentReferenceCodeGenerator();

		assertThat(generator.nextReferenceCode())
				.matches("INC-[0-9A-F]{16}");
	}
}
