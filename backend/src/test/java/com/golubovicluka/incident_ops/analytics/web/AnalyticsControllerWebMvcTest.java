package com.golubovicluka.incident_ops.analytics.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.golubovicluka.incident_ops.analytics.application.GetOperationalSummary;
import com.golubovicluka.incident_ops.analytics.application.dto.OperationalSummaryView;
import com.golubovicluka.incident_ops.shared.config.ApplicationClockConfiguration;
import com.golubovicluka.incident_ops.shared.security.ApiAccessDeniedHandler;
import com.golubovicluka.incident_ops.shared.security.ApiAuthenticationEntryPoint;
import com.golubovicluka.incident_ops.shared.security.JwtConfiguration;
import com.golubovicluka.incident_ops.shared.security.SecurityConfiguration;
import com.golubovicluka.incident_ops.shared.web.ApiErrorWriter;
import com.golubovicluka.incident_ops.shared.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalyticsController.class)
@Import({
		ApplicationClockConfiguration.class,
		ApiAccessDeniedHandler.class,
		ApiAuthenticationEntryPoint.class,
		ApiErrorWriter.class,
		ApiExceptionHandler.class,
		JwtConfiguration.class,
		SecurityConfiguration.class
})
@ActiveProfiles("test")
class AnalyticsControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GetOperationalSummary getOperationalSummary;

	@Test
	@WithMockUser(roles = "RESPONDER")
	void responderReadsOperationalSummary() throws Exception {
		given(getOperationalSummary.execute())
				.willReturn(new OperationalSummaryView(3, 4, 5, 2));

		mockMvc.perform(get("/api/analytics/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.open").value(3))
				.andExpect(jsonPath("$.active").value(4))
				.andExpect(jsonPath("$.resolved").value(5))
				.andExpect(jsonPath("$.breached").value(2));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void administratorReadsOperationalSummary() throws Exception {
		given(getOperationalSummary.execute())
				.willReturn(new OperationalSummaryView(1, 2, 3, 0));

		mockMvc.perform(get("/api/analytics/summary"))
				.andExpect(status().isOk());
	}

	@Test
	void anonymousUserCannotReadOperationalSummary() throws Exception {
		mockMvc.perform(get("/api/analytics/summary"))
				.andExpect(status().isUnauthorized());
	}
}
