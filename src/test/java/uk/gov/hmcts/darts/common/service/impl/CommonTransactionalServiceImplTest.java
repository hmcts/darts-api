package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonTransactionalServiceImplTest {

    @Mock
    CourtroomRepository courtroomRepository;

    @InjectMocks
    RetrieveCoreObjectServiceImpl retrieveCoreObjectServiceImpl;

    @Test
    void courtroomAlreadyExists() {
        when(courtroomRepository.findByNameAndId(
              anyInt(),
              anyString()
        )).thenReturn(Optional.of(CommonTestDataUtil.createCourtroom(
              "test result")));

        CourthouseEntity courthouse = CommonTestDataUtil.createCourthouse("test");
        courthouse.setId(100);
        CourtroomEntity createdCourtroom = retrieveCoreObjectServiceImpl.retrieveOrCreateCourtroom(
              courthouse,
              "Courtroom Name 1"
        );
        assertEquals("test result", createdCourtroom.getName());
    }
}
