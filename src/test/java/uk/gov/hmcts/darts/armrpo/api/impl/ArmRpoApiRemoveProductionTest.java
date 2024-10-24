package uk.gov.hmcts.darts.armrpo.api.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ArmRpoApiRemoveProductionTest {

    @InjectMocks
    private ArmRpoApiImpl armRpoApi;

    @Test
    void removeProduction() {
        assertThrows(NotImplementedException.class, () -> armRpoApi.getExtendedProductionsByMatter("token", 1));
    }

}