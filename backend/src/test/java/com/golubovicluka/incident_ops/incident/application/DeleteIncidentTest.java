package com.golubovicluka.incident_ops.incident.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.incident.application.port.IncidentDeletionCleanup;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentNotFoundException;
import com.golubovicluka.incident_ops.incident.domain.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteIncidentTest {

	@Mock
	private IncidentRepository incidents;

	@Mock
	private IncidentDeletionCleanup dependentRecordCleanup;

	@Mock
	private Incident incident;

	private DeleteIncident deleteIncident;

	@BeforeEach
	void setUp() {
		deleteIncident = new DeleteIncident(
				incidents,
				List.of(dependentRecordCleanup));
	}

	@Test
	void deletesDependentRecordsBeforeTheIncidentAggregate() {
		given(incidents.findById(42L)).willReturn(Optional.of(incident));

		deleteIncident.execute(42L);

		InOrder deletionOrder = inOrder(dependentRecordCleanup, incidents);
		deletionOrder.verify(dependentRecordCleanup).deleteForIncident(42L);
		deletionOrder.verify(incidents).delete(incident);
	}

	@Test
	void rejectsUnknownIncidentWithoutDeletingDependents() {
		given(incidents.findById(404L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> deleteIncident.execute(404L))
				.isInstanceOf(IncidentNotFoundException.class);

		verify(dependentRecordCleanup, never()).deleteForIncident(404L);
		verify(incidents, never()).delete(incident);
	}
}
