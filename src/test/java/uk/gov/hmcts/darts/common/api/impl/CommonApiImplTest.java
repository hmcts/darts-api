package uk.gov.hmcts.darts.common.api.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.CommonCourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.service.CommonTransactionalService;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommonApiImplTest {

    @Mock
    CourtroomRepository courtroomRepository;

    @Mock
    CommonCourthouseRepository courthouseRepository;

    @InjectMocks
    CommonApiImpl commonApi;

    @Mock
    CommonTransactionalService commonTransactionalService;

    @Test
    void testCourtroomExists() {
        CourtroomEntity courtroom1 = CommonTestDataUtil.createCourtroom("1");

        Mockito.when(courtroomRepository.findByNames(anyString(), anyString())).thenReturn(courtroom1);

        CourtroomEntity courtroom = commonApi.retrieveOrCreateCourtroom("SWANSEA", "1");
        assertEquals("1", courtroom.getName());
        verify(courthouseRepository, never()).findByCourthouseNameIgnoreCase(anyString());
    }

    @Test
    void testCreateCourtroom() {
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("SWANSEA");

        Mockito.when(courtroomRepository.findByNames(anyString(), anyString())).thenReturn(null);
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(anyString())).thenReturn(courthouse);
        Mockito.when(commonTransactionalService.createCourtroom(any(CourthouseEntity.class), anyString()))
            .thenReturn(CommonTestDataUtil.createCourtroom("1"));


        CourtroomEntity courtroom = commonApi.retrieveOrCreateCourtroom("SWANSEA", "1");
        assertEquals("1", courtroom.getName());
    }

    @Test
    void testInvalidCourthouse() {

        Mockito.when(courtroomRepository.findByNames(anyString(), anyString())).thenReturn(null);
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(anyString())).thenReturn(null);

        DartsException thrownException = assertThrows(
            DartsException.class,
            () -> commonApi.retrieveOrCreateCourtroom("SWANSEA1", "1")
        );

        assertEquals("Courthouse 'SWANSEA1' not found.", thrownException.getMessage());

    }

}
