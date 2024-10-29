package uk.gov.hmcts.darts.arm.rpo.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ArmRpoApiCreateExportBasedOnSearchResultsTableTest {

    @InjectMocks
    private ArmRpoApiImpl armRpoApi;

    @Test
    void createExportBasedOnSearchResultsTable() {
        assertThrows(NotImplementedException.class, () -> armRpoApi.createExportBasedOnSearchResultsTable("token", 1, Collections.emptyList(), null));
    }

}