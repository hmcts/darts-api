package uk.gov.hmcts.darts.hearings.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.hearings.exception.HearingApiError;
import uk.gov.hmcts.darts.hearings.mapper.AdminHearingMapper;
import uk.gov.hmcts.darts.hearings.model.HearingsResponse;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchRequest;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchResponse;
import uk.gov.hmcts.darts.hearings.service.HearingsService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class AdminHearingServiceTest {
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private HearingsService hearingsService;

    @InjectMocks
    private AdminHearingsServiceImpl adminHearingsService;

    @BeforeEach
    public void before() {
        adminHearingsService.setAdminSearchMaxResults(3);
    }

    @Test
    void testSearchForHearingsWithMaximumRecordsReturned() {
        List<HearingEntity> hearingEntityList = new ArrayList<>();
        hearingEntityList.add(setupHearing(1));
        hearingEntityList.add(setupHearing(2));
        hearingEntityList.add(setupHearing(3));


        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(1);
        String courtroomName = "mycourtroom";
        String caseNumber = "casenumber";
        List<Integer> courthouseidsLst = List.of(2, 3, 4);


        when(hearingRepository.findHearingDetails(courthouseidsLst,
                                                  caseNumber,
                                                  courtroomName, startDate, endDate, 4)).thenReturn(hearingEntityList);


        HearingsSearchRequest hearingsSearchRequest = new HearingsSearchRequest();
        hearingsSearchRequest.setCourthouseIds(courthouseidsLst);
        hearingsSearchRequest.setHearingStartAt(startDate);
        hearingsSearchRequest.setHearingEndAt(endDate);
        hearingsSearchRequest.setCaseNumber(caseNumber);
        hearingsSearchRequest.setCourtroomName(courtroomName);

        List<HearingsSearchResponse> actualResponse = adminHearingsService.adminHearingSearch(hearingsSearchRequest);
        assertEquals(hearingEntityList.size(), actualResponse.size());

        for (int i = 0; i < hearingEntityList.size(); i++) {
            assertEquals(hearingEntityList.get(i).getHearingDate(), actualResponse.get(i).getHearingDate());
            assertEquals(hearingEntityList.get(i).getId(), actualResponse.get(i).getId());
            assertEquals(hearingEntityList.get(i).getCourtroom().getCourthouse().getId(), actualResponse.get(i).getCourthouse().getId());
            assertEquals(hearingEntityList.get(i).getCourtroom().getCourthouse().getDisplayName(), actualResponse.get(i).getCourthouse().getDisplayName());
            assertEquals(hearingEntityList.get(i).getCourtroom().getId(), actualResponse.get(i).getCourtroom().getId());
            assertEquals(hearingEntityList.get(i).getCourtroom().getName(), actualResponse.get(i).getCourtroom().getName());
            assertEquals(hearingEntityList.get(i).getCourtCase().getId(), actualResponse.get(i).getCase().getId());
            assertEquals(hearingEntityList.get(i).getCourtCase().getCaseNumber(), actualResponse.get(i).getCase().getCaseNumber());
        }
    }

    @Test
    void testSearchForHearingsWithMaximumRecordsExceeded() {

        List<HearingEntity> hearingEntityList = new ArrayList<>();
        hearingEntityList.add(setupHearing(1));
        hearingEntityList.add(setupHearing(2));
        hearingEntityList.add(setupHearing(3));
        hearingEntityList.add(setupHearing(4));

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(1);
        String courtroomName = "mycourtroom";
        String caseNumber = "casenumber";
        List<Integer> courthouseidsLst = List.of(2, 3, 4);

        when(hearingRepository.findHearingDetails(courthouseidsLst,
                                                  caseNumber,
                                                  courtroomName, startDate, endDate, 4)).thenReturn(hearingEntityList);


        HearingsSearchRequest hearingsSearchRequest = new HearingsSearchRequest();
        hearingsSearchRequest.setCourthouseIds(courthouseidsLst);
        hearingsSearchRequest.setHearingStartAt(startDate);
        hearingsSearchRequest.setHearingEndAt(endDate);
        hearingsSearchRequest.setCaseNumber(caseNumber);
        hearingsSearchRequest.setCourtroomName(courtroomName);

        DartsApiException actualException = assertThrows(DartsApiException.class, () -> adminHearingsService.adminHearingSearch(hearingsSearchRequest));
        assertEquals(HearingApiError.TOO_MANY_RESULTS, actualException.getError());
    }

    @Test
    void getAdminHearings_shouldReturnHearingResponse() {
        HearingsResponse expectedHearingsResponse = mock(HearingsResponse.class);
        HearingEntity hearingEntity = mock(HearingEntity.class);
        when(hearingsService.getHearingById(123)).thenReturn(hearingEntity);
        try (MockedStatic<AdminHearingMapper> adminHearingMapperMockedStatic = Mockito.mockStatic(AdminHearingMapper.class)) {
            adminHearingMapperMockedStatic.when(() -> AdminHearingMapper.mapToHearingsResponse(hearingEntity)).thenReturn(expectedHearingsResponse);

            assertThat(adminHearingsService.getAdminHearings(123)).isEqualTo(expectedHearingsResponse);
            verify(hearingsService).getHearingById(123);
            adminHearingMapperMockedStatic.verify(() -> AdminHearingMapper.mapToHearingsResponse(hearingEntity));
        }
    }

    public static HearingEntity setupHearing(Integer id) {
        return setupHearing(id, id, id, id);
    }

    public static HearingEntity setupHearing(Integer id, Integer courthouseId, Integer courtroomId, Integer courtcaseId) {

        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setId(courthouseId);
        courthouseEntity.setDisplayName("Display name " + courthouseId);

        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setId(courtroomId);
        courtroomEntity.setName("Name " + courtcaseId);

        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(courtcaseId);
        courtCaseEntity.setCaseNumber("Case number" + courtcaseId);

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setId(id);
        hearingEntity.setHearingDate(LocalDate.now());
        hearingEntity.setCourtCase(courtCaseEntity);
        hearingEntity.setCourtroom(courtroomEntity);

        courtroomEntity.setCourthouse(courthouseEntity);

        return hearingEntity;
    }

}