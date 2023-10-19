package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrieveCoreObjectServiceImplTest {

    private static final String COURTHOUSE_1 = "courthouse1";
    private static final String COURTROOM_1 = "courtroom1";
    private static final String CASE_NUMBER_1 = "caseNumber1";
    @Mock
    HearingRepository hearingRepository;

    @Mock
    CaseRepository caseRepository;

    @Mock
    CourthouseRepository courthouseRepository;

    @Mock
    CourtroomRepository courtroomRepository;

    @InjectMocks
    RetrieveCoreObjectServiceImpl retrieveCoreObjectServiceImpl;

    @Test
    void hearingExists() {
        HearingEntity hearingToBeFound = new HearingEntity();
        hearingToBeFound.setId(123);
        when(hearingRepository.findHearing(anyString(), anyString(), anyString(), any(LocalDate.class))).thenReturn(
            Optional.of(hearingToBeFound));

        HearingEntity response = retrieveCoreObjectServiceImpl.retrieveOrCreateHearing(
            COURTHOUSE_1,
            COURTROOM_1,
            CASE_NUMBER_1,
            LocalDate.now()
        );

        assertEquals(123, response.getId());
    }

    @Test
    void hearingCreate() {
        mockCourthouse();
        when(hearingRepository.findHearing(anyString(), anyString(), anyString(), any(LocalDate.class))).thenReturn(
            Optional.empty());

        HearingEntity response = retrieveCoreObjectServiceImpl.retrieveOrCreateHearing(
            COURTHOUSE_1,
            COURTROOM_1,
            CASE_NUMBER_1,
            LocalDate.now()
        );

        assertEquals(COURTROOM_1, response.getCourtroom().getName());
        assertEquals(1, response.getCourtroom().getCourthouse().getId());
        assertEquals(CASE_NUMBER_1, response.getCourtCase().getCaseNumber());
    }

    @Test
    void courtroomCreate() {
        mockCourthouse();
        when(courtroomRepository.findByCourthouseNameAndCourtroomName(anyString(), anyString())).thenReturn(
            Optional.empty());

        CourtroomEntity response = retrieveCoreObjectServiceImpl.retrieveOrCreateCourtroom(
            COURTHOUSE_1,
            COURTROOM_1
        );

        assertEquals(COURTROOM_1, response.getName());
        assertEquals(1, response.getCourthouse().getId());
    }

    @Test
    void courtroomRetrieve() {

        CourtroomEntity courtroom = CommonTestDataUtil.createCourtroom(COURTROOM_1);
        when(courtroomRepository.findByCourthouseNameAndCourtroomName(anyString(), anyString())).thenReturn(
            Optional.of(courtroom));

        CourtroomEntity response = retrieveCoreObjectServiceImpl.retrieveOrCreateCourtroom(
            COURTHOUSE_1,
            COURTROOM_1
        );

        assertEquals(COURTROOM_1, response.getName());
    }

    @Test
    void hearingCreateMissingCourthouse() {
        HearingEntity hearingToBeFound = new HearingEntity();
        hearingToBeFound.setId(123);
        when(hearingRepository.findHearing(anyString(), anyString(), anyString(), any(LocalDate.class))).thenReturn(
            Optional.empty());

        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> retrieveCoreObjectServiceImpl.retrieveOrCreateHearing(
                COURTHOUSE_1,
                COURTROOM_1,
                CASE_NUMBER_1,
                LocalDate.now()
            )
        );

        assertEquals("Provided courthouse does not exist. Courthouse 'courthouse1' not found.", exception.getMessage());
    }

    private void mockCourthouse() {
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setId(1);
        courthouse.setCourthouseName("Swansea");
        when(courthouseRepository.findByCourthouseNameIgnoreCase(anyString())).thenReturn(
            Optional.of(courthouse));
    }

}
