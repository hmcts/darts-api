package uk.gov.hmcts.darts.common.api.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.repository.CommonCourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.service.impl.RetrieveCoreObjectServiceImpl;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    RetrieveCoreObjectServiceImpl retrieveCoreObjectService;

    @Test
    void testCourtroomExists() {
        CourtroomEntity courtroom1 = CommonTestDataUtil.createCourtroom("1");

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCourtroom(anyString(), anyString())).thenReturn(
            courtroom1);

        CourtroomEntity courtroom = commonApi.retrieveOrCreateCourtroom("SWANSEA", "1");
        assertEquals("1", courtroom.getName());
        verify(courthouseRepository, never()).findByCourthouseNameIgnoreCase(anyString());
    }

    @Test
    void testCreateCourtroom() {
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCourtroom(anyString(), anyString()))
            .thenReturn(CommonTestDataUtil.createCourtroom("1"));


        CourtroomEntity courtroom = commonApi.retrieveOrCreateCourtroom("SWANSEA", "1");
        assertEquals("1", courtroom.getName());
    }

}
