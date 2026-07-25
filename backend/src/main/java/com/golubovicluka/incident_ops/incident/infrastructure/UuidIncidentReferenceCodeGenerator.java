package com.golubovicluka.incident_ops.incident.infrastructure;

import java.util.Locale;
import java.util.UUID;

import com.golubovicluka.incident_ops.incident.application.ReferenceCodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class UuidIncidentReferenceCodeGenerator
		implements ReferenceCodeGenerator {

	@Override
	public String nextReferenceCode() {
		return "INC-" + UUID.randomUUID()
				.toString()
				.replace("-", "")
				.substring(0, 16)
				.toUpperCase(Locale.ROOT);
	}
}
