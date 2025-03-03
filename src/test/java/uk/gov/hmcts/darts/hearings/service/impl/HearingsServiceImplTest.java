package uk.gov.hmcts.darts.hearings.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.hearings.exception.HearingApiError;
import uk.gov.hmcts.darts.hearings.mapper.GetHearingResponseMapper;
import uk.gov.hmcts.darts.hearings.mapper.HearingTranscriptionMapper;
import uk.gov.hmcts.darts.hearings.model.EventResponse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingsServiceImplTest {

    @Mock
    HearingRepository hearingRepository;
    @Mock
    MediaLinkedCaseRepository mediaLinkedCaseRepository;
    @Mock
    EventRepository eventRepository;
    @Mock
    HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;

    @Mock
    TranscriptionRepository transcriptionRepository;

    @Mock
    AnnotationRepository annotationRepository;

    @Mock
    AuthorisationApi authorisationApi;
    @Mock
    MediaRepository mediaRepository;

    HearingsServiceImpl service;

    GetHearingResponseMapper getHearingResponseMapper;

    @BeforeEach
    void setUp() {
        service = new HearingsServiceImpl(
            getHearingResponseMapper,
            hearingRepository,
            mediaLinkedCaseRepository,
            transcriptionRepository,
            eventRepository,
            annotationRepository,
            authorisationApi,
            new HearingTranscriptionMapper(),
            mediaRepository
        );
    }

    @Test
    void testGetHearingsById() {
        HearingEntity hearingEntity = createHearingEntity(true);
        when(hearingRepository.findById(hearingEntity.getId())).thenReturn(Optional.of(hearingEntity));

        HearingEntity result = service.getHearingByIdWithValidation(hearingEntity.getId());
        assertEquals(1, result.getId());
    }

    @Test
    void testGetHearingsByIdHearingNotFound() {
        when(hearingRepository.findById(1)).thenReturn(Optional.empty());

        DartsApiException exception = assertThrows(DartsApiException.class, () -> service.getHearingByIdWithValidation(1));

        assertEquals("HEARING_100", exception.getError().getErrorTypeNumeric());
    }

    @Test
    void testGetHearingsByIdCaseIsExpired() {
        HearingEntity hearingEntity = createHearingEntity(true);
        hearingEntity.getCourtCase().setDataAnonymised(true);
        when(hearingRepository.findById(hearingEntity.getId())).thenReturn(Optional.of(hearingEntity));

        DartsApiException exception = assertThrows(DartsApiException.class, () -> service.getHearingByIdWithValidation(1));

        assertThat(exception.getError()).isEqualTo(CaseApiError.CASE_EXPIRED);
        assertThat(exception.getMessage()).isEqualTo("Case has expired.");
    }

    @Test
    void testGetHearingsByIdHearingNotActual() {
        HearingEntity hearingEntity = createHearingEntity(false);
        when(hearingRepository.findById(hearingEntity.getId())).thenReturn(Optional.of(hearingEntity));

        DartsApiException exception = assertThrows(DartsApiException.class, () -> service.getHearingByIdWithValidation(hearingEntity.getId()));

        assertEquals("HEARING_102", exception.getError().getErrorTypeNumeric());
    }

    @Test
    void testGetEventsResponse() {
        HearingEntity hearingEntity = createHearingEntity(true);

        EventHandlerEntity eventType = mock(EventHandlerEntity.class);
        when(eventType.getEventName()).thenReturn("TestEvent");

        List<EventEntity> event = List.of(
            CommonTestDataUtil.createEventWith("Test", hearingEntity, eventType));
        when(eventRepository.findAllByHearingId(hearingEntity.getId())).thenReturn(event);

        List<EventResponse> eventResponses = service.getEvents(hearingEntity.getId());
        assertEquals(1, eventResponses.size());
        assertEquals(event.get(0).getId(), eventResponses.get(0).getId());
        assertEquals("Test", eventResponses.get(0).getText());
        assertEquals("TestEvent", eventResponses.get(0).getName());
        assertNotNull(eventResponses.get(0).getTimestamp());

    }

    @Test
    void testGetEventsResponseCaseIsExpired() {
        HearingEntity hearingEntity = createHearingEntity(true);
        hearingEntity.getCourtCase().setDataAnonymised(true);

        when(hearingRepository.findById(hearingEntity.getId())).thenReturn(Optional.of(hearingEntity));

        DartsApiException exception = assertThrows(DartsApiException.class, () -> service.getEvents(hearingEntity.getId()));

        assertThat(exception.getError()).isEqualTo(CaseApiError.CASE_EXPIRED);
        assertThat(exception.getMessage()).isEqualTo("Case has expired.");
    }

    @Test
    void removeMediaLinkToHearing_shouldRemoveLinkToHearing_whenAllAssociatedCasesAreAnonymised() {
        MediaEntity mediaEntity = CommonTestDataUtil.createMedia("T1234");
        HearingEntity hearingEntity = mediaEntity.getHearingList().getFirst();
        hearingEntity.setMediaList(new ArrayList<>(List.of(mediaEntity)));

        when(mediaRepository.findByCaseIdWithMediaList(hearingEntity.getCourtCase().getId())).thenReturn(List.of(mediaEntity));
        when(mediaLinkedCaseRepository.areAllAssociatedCasesAnonymised(any())).thenReturn(true);

        service.removeMediaLinkToHearing(hearingEntity.getCourtCase().getId());
        verify(mediaRepository).save(mediaEntity);
    }

    @Test
    void removeMediaLinkToHearing_shouldNotRemoveLinkToHearing_whenAllAssociatedCasesAreNotAnonymised() {
        MediaEntity mediaEntity = CommonTestDataUtil.createMedia("T1234");
        HearingEntity hearingEntity = mediaEntity.getHearingList().getFirst();
        hearingEntity.setMediaList(List.of(mediaEntity));

        when(mediaRepository.findByCaseIdWithMediaList(hearingEntity.getCourtCase().getId())).thenReturn(List.of(mediaEntity));
        when(mediaLinkedCaseRepository.areAllAssociatedCasesAnonymised(any())).thenReturn(false);

        service.removeMediaLinkToHearing(hearingEntity.getCourtCase().getId());
        verifyNoMoreInteractions(hearingRepository, mediaRepository);
    }

    @Test
    void removeMediaLinkToHearing_shouldDoNothing_whenHearingsWithNoMediaExistsToUnlink() {
        MediaEntity mediaEntity = CommonTestDataUtil.createMedia("T1234");
        HearingEntity hearingEntity = mediaEntity.getHearingList().getFirst();

        when(mediaRepository.findByCaseIdWithMediaList(hearingEntity.getCourtCase().getId())).thenReturn(List.of(mediaEntity));

        service.removeMediaLinkToHearing(hearingEntity.getCourtCase().getId());
        verifyNoMoreInteractions(hearingRepository, mediaRepository);
    }

    @Test
    void removeMediaLinkToHearing_shouldDoNothing_whenNoHearingsExist() {
        HearingEntity hearingEntity = createHearingEntity(true);

        when(mediaRepository.findByCaseIdWithMediaList(hearingEntity.getCourtCase().getId())).thenReturn(List.of());

        service.removeMediaLinkToHearing(hearingEntity.getCourtCase().getId());
        verifyNoMoreInteractions(hearingRepository, mediaRepository);
    }


    @Test
    void getHearingById_hearingFound_shouldReturnHearing() {
        HearingEntity hearingEntity = createHearingEntity(true);
        when(hearingRepository.findById(hearingEntity.getId())).thenReturn(Optional.of(hearingEntity));

        HearingEntity result = service.getHearingById(hearingEntity.getId());
        assertEquals(hearingEntity, result);
    }

    @Test
    void getHearingById_hearingNotFound_exceptionShouldBeThrown() {
        when(hearingRepository.findById(1)).thenReturn(Optional.empty());

        DartsApiException exception = assertThrows(DartsApiException.class, () -> service.getHearingById(1));
        assertEquals(HearingApiError.HEARING_NOT_FOUND, exception.getError());
    }


    private HearingEntity createHearingEntity(boolean isHearingActual) {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse("swansea");
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CourtCaseEntity caseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        caseEntity.setId(1);

        return CommonTestDataUtil.createHearing(caseEntity, courtroomEntity, LocalDate.now(), isHearingActual);
    }


}