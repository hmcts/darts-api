package uk.gov.hmcts.darts.arm.rpo.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ArmRpoApiGetExtendedSearchesByMatterTest {

    @InjectMocks
    private ArmRpoApiImpl armRpoApi;

    @Test
    void getExtendedSearchesByMatter() {
        assertThrows(NotImplementedException.class, () -> armRpoApi.getExtendedSearchesByMatter("token", 1, null));
    }


}