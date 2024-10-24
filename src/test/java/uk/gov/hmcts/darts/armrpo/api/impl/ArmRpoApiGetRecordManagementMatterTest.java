package uk.gov.hmcts.darts.armrpo.api.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ArmRpoApiGetRecordManagementMatterTest {

    @InjectMocks
    private ArmRpoApiImpl armRpoApi;

    @Test
    void getRecordManagementMatter() {
        assertThrows(NotImplementedException.class, () -> armRpoApi.getRecordManagementMatter("token", 1));
    }

}