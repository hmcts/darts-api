package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.common.service.HearingCommonService;
import uk.gov.hmcts.darts.event.exception.EventError;
import uk.gov.hmcts.darts.event.mapper.EventMapper;
import uk.gov.hmcts.darts.event.model.AdminGetVersionsByEventIdResponseResult;
import uk.gov.hmcts.darts.event.model.PatchAdminEventByIdRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventMapper eventMapper;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private DataAnonymisationService dataAnonymisationService;
    @Mock
    private EventLinkedCaseRepository eventLinkedCaseRepository;

    @Mock
    private HearingCommonService hearingCommonService;
    @Mock
    private AuditApi auditApi;
    @Mock
    private UserIdentity userIdentity;

    @InjectMocks
    @Spy
    private EventServiceImpl eventService;


    @Test
    void positiveGetEventEntityById() {
        EventEntity event = mock(EventEntity.class);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        assertThat(eventService.getEventByEveId(1L)).isEqualTo(event);
        verify(eventRepository, times(1)).findById(1L);
    }


    @Test
    void positiveGetEventEntityByIdNotFound() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventByEveId(1L))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", CommonApiError.NOT_FOUND);
        verify(eventRepository, times(1)).findById(1L);
    }

    @Test
    void positiveSaveEvent() {
        EventEntity event = mock(EventEntity.class);
        when(eventRepository.save(event)).thenReturn(event);
        assertThat(eventService.saveEvent(event)).isEqualTo(event);
        verify(eventRepository, times(1)).save(event);
    }

    @Test
    void positiveGetAllCourtCaseEventVersions() {
        EventLinkedCaseEntity eventLinkedCase1 = new EventLinkedCaseEntity();
        EventEntity event1 = mock(EventEntity.class);
        eventLinkedCase1.setEvent(event1);
        EventLinkedCaseEntity eventLinkedCase2 = new EventLinkedCaseEntity();
        EventEntity event2 = mock(EventEntity.class);
        eventLinkedCase2.setEvent(event2);
        EventLinkedCaseEntity eventLinkedCase3 = new EventLinkedCaseEntity();
        EventEntity event3 = mock(EventEntity.class);
        eventLinkedCase3.setEvent(event3);
        when(eventLinkedCaseRepository.findAllByCourtCase(any())).thenReturn(List.of(eventLinkedCase1, eventLinkedCase2));
        when(eventLinkedCaseRepository.findAllByCaseNumberAndCourthouseName(any(), any())).thenReturn(List.of(eventLinkedCase2, eventLinkedCase3));
        CourtCaseEntity courtCase = mock(CourtCaseEntity.class, RETURNS_DEEP_STUBS);
        when(courtCase.getCaseNumber()).thenReturn("caseNumber");
        when(courtCase.getCourthouse().getCourthouseName()).thenReturn("courthouseName");

        Set<EventEntity> result = eventService.getAllCourtCaseEventVersions(courtCase);

        assertThat(result).containsExactlyInAnyOrder(event1, event2, event3);
        verify(eventLinkedCaseRepository).findAllByCourtCase(courtCase);
        verify(eventLinkedCaseRepository).findAllByCaseNumberAndCourthouseName("caseNumber", "courthouseName");
    }


    @Test
    @DisplayName("areAllAssociatedCasesAnonymised(...) method, should return true if all associated cases are anonymised")
    void allAssociatedCasesAnonymisedTrue() {
        EventEntity event = mock(EventEntity.class);
        when(eventLinkedCaseRepository.areAllAssociatedCasesAnonymised(event)).thenReturn(true);
        assertThat(eventService.allAssociatedCasesAnonymised(event)).isTrue();
        verify(eventLinkedCaseRepository).areAllAssociatedCasesAnonymised(event);
    }

    @Test
    void positiveAllAssociatedCasesAnonymisedFalse() {
        EventEntity event = mock(EventEntity.class);
        when(eventLinkedCaseRepository.areAllAssociatedCasesAnonymised(event)).thenReturn(false);
        assertThat(eventService.allAssociatedCasesAnonymised(event)).isFalse();
        verify(eventLinkedCaseRepository).areAllAssociatedCasesAnonymised(event);
    }

    @Test
    void adminGetVersionsByEventId_shouldReturnEvent() {
        List<EventEntity> eventEntities = List.of(mock(EventEntity.class), mock(EventEntity.class), mock(EventEntity.class));
        doReturn(eventEntities).when(eventService).getRelatedEvents(123L);

        AdminGetVersionsByEventIdResponseResult responseDetails = mock(AdminGetVersionsByEventIdResponseResult.class);
        when(eventMapper.mapToAdminGetEventVersionsResponseForId(any())).thenReturn(responseDetails);


        assertThat(eventService.adminGetVersionsByEventId(123L)).isEqualTo(responseDetails);

        verify(eventService).getRelatedEvents(123L);
        verify(eventMapper)
            .mapToAdminGetEventVersionsResponseForId(eventEntities);
    }

    @Test
    void getRelatedEvents_eventIdIsZero() {
        EventEntity event = mock(EventEntity.class);
        when(event.getEventId()).thenReturn(0);
        doReturn(event).when(eventService).getEventByEveId(123L);

        assertThat(eventService.getRelatedEvents(123L)).isEqualTo(List.of(event));
        verifyNoInteractions(eventRepository);
        verify(eventService).getEventByEveId(123L);
    }

    @Test
    void getRelatedEvents_hasSingleEventLinkedCase() {
        CourtCaseEntity caseEntity = mock(CourtCaseEntity.class);
        when(caseEntity.getId()).thenReturn(1);
        EventLinkedCaseEntity eventLinkedCaseEntity = mock(EventLinkedCaseEntity.class);
        when(eventLinkedCaseEntity.getCourtCase()).thenReturn(caseEntity);

        EventEntity event = mock(EventEntity.class);
        when(event.getEventLinkedCaseEntities()).thenReturn(List.of(eventLinkedCaseEntity));
        when(event.getId()).thenReturn(123L);
        when(event.getEventId()).thenReturn(321);
        doReturn(event).when(eventService).getEventByEveId(123L);

        List<EventEntity> eventEntities = List.of(mock(EventEntity.class), mock(EventEntity.class), mock(EventEntity.class));
        when(eventRepository.findAllByRelatedEvents(123L, 321, List.of(1))).thenReturn(eventEntities);

        assertThat(eventService.getRelatedEvents(123L)).isEqualTo(eventEntities);
        verify(eventRepository).findAllByRelatedEvents(123L, 321, List.of(1));
        verify(eventService).getEventByEveId(123L);
    }

    @Test
    void getRelatedEvents_hasMultipleSingleEventLinkedCase() {
        CourtCaseEntity caseEntity1 = mock(CourtCaseEntity.class);
        when(caseEntity1.getId()).thenReturn(1);
        CourtCaseEntity caseEntity2 = mock(CourtCaseEntity.class);
        when(caseEntity2.getId()).thenReturn(2);
        CourtCaseEntity caseEntity3 = mock(CourtCaseEntity.class);
        when(caseEntity3.getId()).thenReturn(3);

        EventLinkedCaseEntity eventLinkedCaseEntity1 = mock(EventLinkedCaseEntity.class);
        when(eventLinkedCaseEntity1.getCourtCase()).thenReturn(caseEntity1);
        EventLinkedCaseEntity eventLinkedCaseEntity2 = mock(EventLinkedCaseEntity.class);
        when(eventLinkedCaseEntity2.getCourtCase()).thenReturn(caseEntity2);
        EventLinkedCaseEntity eventLinkedCaseEntity3 = mock(EventLinkedCaseEntity.class);
        when(eventLinkedCaseEntity3.getCourtCase()).thenReturn(caseEntity3);

        EventEntity event = mock(EventEntity.class);
        when(event.getEventLinkedCaseEntities()).thenReturn(List.of(eventLinkedCaseEntity1, eventLinkedCaseEntity2, eventLinkedCaseEntity3));
        when(event.getId()).thenReturn(123L);
        when(event.getEventId()).thenReturn(321);
        doReturn(event).when(eventService).getEventByEveId(123L);

        List<EventEntity> eventEntities = List.of(mock(EventEntity.class), mock(EventEntity.class), mock(EventEntity.class));
        when(eventRepository.findAllByRelatedEvents(123L, 321, List.of(1, 2, 3))).thenReturn(
            eventEntities);

        assertThat(eventService.getRelatedEvents(123L)).isEqualTo(eventEntities);
        verify(eventRepository).findAllByRelatedEvents(123L, 321, List.of(1, 2, 3));
        verify(eventService).getEventByEveId(123L);
    }

    @Nested
    class PatchEventByIdTests {

        @ParameterizedTest
        @ValueSource(booleans = false)
        @NullSource
        void shouldThrowException_whenIsCurrentIsTrue(Boolean isCurrent) {
            PatchAdminEventByIdRequest request = new PatchAdminEventByIdRequest(isCurrent);
            DartsApiException exception = assertThrows(DartsApiException.class, () -> eventService.patchEventById(1, request));
            assertThat(exception.getError()).isEqualTo(CommonApiError.INVALID_REQUEST);
            verifyNoInteractions(auditApi);
        }

        @Test
        void shouldThrowException_whenMediaIsAlreadyIsCurrent() {
            PatchAdminEventByIdRequest request = new PatchAdminEventByIdRequest(true);
            EventEntity event = mock(EventEntity.class);
            doReturn(event).when(eventService).getEventByEveId(123);
            when(event.isCurrent()).thenReturn(true);
            DartsApiException exception = assertThrows(DartsApiException.class, () -> eventService.patchEventById(123, request));
            assertThat(exception.getError()).isEqualTo(EventError.EVENT_ALREADY_CURRENT);
            verifyNoInteractions(auditApi);
        }

        @Test
        void shouldUpdateMediaIsCurrent_whenMediaIsNotCurrent() {
            UserAccountEntity currentUser = new UserAccountEntity();
            when(userIdentity.getUserAccount()).thenReturn(currentUser);

            EventEntity event = new EventEntity();
            event.setIsCurrent(false);
            event.setId(123);
            doReturn(event).when(eventService).getEventByEveId(123);

            CourtCaseEntity courtCase1 = new CourtCaseEntity();
            CourtCaseEntity courtCase2 = new CourtCaseEntity();
            EventLinkedCaseEntity eventLinkedCaseEntity1 = new EventLinkedCaseEntity();
            eventLinkedCaseEntity1.setCourtCase(courtCase1);
            EventLinkedCaseEntity eventLinkedCaseEntity2 = new EventLinkedCaseEntity();
            eventLinkedCaseEntity2.setCourtCase(courtCase2);

            event.setEventLinkedCaseEntities(List.of(eventLinkedCaseEntity1, eventLinkedCaseEntity2));


            EventEntity oldEvent1IsCurrent = new EventEntity();
            oldEvent1IsCurrent.setIsCurrent(true);
            oldEvent1IsCurrent.setId(1);

            EventEntity oldEvent2IsCurrent = new EventEntity();
            oldEvent2IsCurrent.setIsCurrent(true);
            oldEvent2IsCurrent.setId(2);

            //Should not call deleteEventLinkingAndSetCurrentFalse as not current
            EventEntity oldEvent3IsNotCurrent = new EventEntity();
            oldEvent3IsNotCurrent.setIsCurrent(false);
            oldEvent3IsNotCurrent.setId(3);

            List<EventEntity> eventEntities = new ArrayList<>();
            eventEntities.add(oldEvent1IsCurrent);
            eventEntities.add(oldEvent2IsCurrent);
            eventEntities.add(oldEvent3IsNotCurrent);
            eventEntities.add(event);
            doReturn(eventEntities).when(eventService).getRelatedEvents(event);

            PatchAdminEventByIdRequest request = new PatchAdminEventByIdRequest(true);
            doNothing().when(eventService).deleteEventLinkingAndSetCurrentFalse(any());
            eventService.patchEventById(123, request);

            verify(eventService).getRelatedEvents(event);
            verify(eventService).deleteEventLinkingAndSetCurrentFalse(oldEvent1IsCurrent);
            verify(eventService).deleteEventLinkingAndSetCurrentFalse(oldEvent2IsCurrent);

            verify(hearingCommonService).linkEventToHearings(courtCase1, event);
            verify(hearingCommonService).linkEventToHearings(courtCase2, event);
            verify(eventRepository).save(event);

            assertThat(event.isCurrent()).isEqualTo(true);
            verify(eventService).patchEventById(123, request);//Required for verifyNoMoreInteractions
            verifyNoMoreInteractions(eventService, hearingCommonService);
            verify(auditApi)
                .record(
                    AuditActivity.CURRENT_EVENT_VERSION_UPDATED,
                    currentUser,
                    "eve_id: 123 was made current replacing eve_id: [1, 2]"
                );
        }
    }

    @Test
    void deleteEventLinkingAndSetCurrentFalse_shouldRemoveAllAssciatedHearingsAndSetIsCurrentToFalse_whenEventEntityIsGiven() {
        EventEntity event = new EventEntity();
        event.addHearing(new HearingEntity());
        event.addHearing(new HearingEntity());
        event.addHearing(new HearingEntity());
        event.setIsCurrent(true);

        assertThat(event.isCurrent()).isTrue();
        assertThat(event.getHearingEntities()).hasSize(3);

        eventService.deleteEventLinkingAndSetCurrentFalse(event);

        assertThat(event.isCurrent()).isFalse();
        assertThat(event.getHearingEntities()).isEmpty();
    }
}