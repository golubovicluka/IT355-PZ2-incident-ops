package com.golubovicluka.incident_ops;

import com.golubovicluka.incident_ops.integration.PostgreSQLContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.main.web-application-type=none"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IncidentOpsApplicationTests extends PostgreSQLContainerSupport {

	@Test
	void contextLoads() {
	}

}
