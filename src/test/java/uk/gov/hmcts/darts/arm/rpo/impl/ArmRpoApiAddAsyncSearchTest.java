package uk.gov.hmcts.darts.arm.rpo.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ArmRpoApiAddAsyncSearchTest {

    @InjectMocks
    private ArmRpoApiImpl armRpoApi;

    @Test
    void addAsyncSearch() {
        assertThrows(NotImplementedException.class, () -> armRpoApi.addAsyncSearch("token", 1));
    }

}