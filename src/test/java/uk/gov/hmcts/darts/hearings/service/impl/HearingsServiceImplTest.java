package uk.gov.hmcts.darts.hearings.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.hearings.model.EventResponse;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class HearingsServiceImplTest {

    @Mock
    HearingRepository hearingRepository;
    @Mock
    EventRepository eventRepository;


    HearingsServiceImpl service;


    @BeforeEach
    void setUp() {
        service = new HearingsServiceImpl(
            hearingRepository,
            eventRepository
        );
    }

    @Test
    void testGetEventsResponse() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse("swansea");
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CourtCaseEntity caseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        caseEntity.setId(1);

        HearingEntity hearingEntity = CommonTestDataUtil.createHearing(
            caseEntity,
            courtroomEntity,
            LocalDate.now()
        );

        EventHandlerEntity eventType = mock(EventHandlerEntity.class);
        Mockito.when(eventType.getEventName()).thenReturn("TestEvent");

        List<EventEntity> event = List.of(
            CommonTestDataUtil.createEventWith("LOG", "Test", hearingEntity, eventType));
        Mockito.when(eventRepository.findAllByHearingId(hearingEntity.getId())).thenReturn(event);

        List<EventResponse> eventResponses = service.getEvents(hearingEntity.getId());
        assertEquals(1, eventResponses.size());
        assertEquals(event.get(0).getId(), eventResponses.get(0).getId());
        assertEquals("Test", eventResponses.get(0).getText());
        assertEquals("TestEvent", eventResponses.get(0).getName());
        assertNotNull(eventResponses.get(0).getTimestamp());

    }

}
