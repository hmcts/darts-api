package uk.gov.hmcts.darts.event.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.CPP;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.XHIBIT;
import static uk.gov.hmcts.darts.testutils.data.EventHandlerTestData.createEventHandlerWith;

@AutoConfigureMockMvc
@Transactional
class EventsControllerPostEventsTest extends IntegrationBase {


    private static final URI ENDPOINT = URI.create("/events");
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";

    @Autowired
    private EventHandlerRepository eventHandlerRepository;
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;

    @BeforeEach
    void setUp() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            SOME_DATE_TIME.toLocalDate()
        );
    }

    @Test
    void eventsPostShouldPersistActiveDescriptionForEventHandler() throws Exception {
        EventHandlerEntity activeHandler = getActiveHandler();
        activeHandler.setEventName("New Description");

        EventHandlerEntity inactiveHandler = getInactiveHandler();
        inactiveHandler.setEventName("Old Description");

        dartsDatabase.saveAll(activeHandler, inactiveHandler);

        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("swansea");
        courthouse.setDisplayName("swansea");

        courthouse = dartsDatabase.getCourthouseRepository().save(courthouse);

        String requestBody = """
            {
              "message_id": "12345",
              "type": "ActiveTestType",
              "sub_type": "ActiveTestSubType",
              "courthouse": "swansea",
              "courtroom": "1",
              "case_numbers": [
                "A20230049"
              ],
              "date_time": "2023-06-14T08:37:30.945Z"
            }""";

        setupExternalUserForCourthouse(courthouse);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(requestBody);

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isCreated());

        List<EventEntity> results = dartsDatabase.getAllEvents()
            .stream()
            .filter(eventEntity -> "ActiveTestType".equals(eventEntity.getEventType().getType()))
            .toList();

        Assertions.assertEquals(1, results.size());
        EventEntity persistedEvent = results.get(0);

        EventHandlerEntity eventType = persistedEvent.getEventType();
        Assertions.assertEquals("New Description", eventType.getEventName());

        dartsDatabase.addToTrash(activeHandler, inactiveHandler);
    }

    @Test
    void useExistingCase() throws Exception {
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("swansea1");
        courthouse.setDisplayName("swansea1");

        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber("CaseNumber");
        courtCase.setClosed(false);
        courtCase.setInterpreterUsed(false);
        courtCase.setCourthouse(courthouse);
        dartsDatabase.getCaseRepository().save(courtCase);

        String requestBody = """
            {
              "message_id": "useExistingCase",
              "type": "1000",
              "sub_type": "1002",
              "courthouse": "swansea1",
              "courtroom": "1",
              "case_numbers": [
                "casenumber"
              ],
              "date_time": "2023-06-14T08:37:30.945Z"
            }""";

        setupExternalUserForCourthouse(courthouse);

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(requestBody);

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isCreated());

        List<EventEntity> results = dartsDatabase.getAllEvents()
            .stream()
            .filter(eventEntity -> "useExistingCase".equals(eventEntity.getMessageId()))
            .toList();

        Assertions.assertEquals(1, results.size());

    }

    @Test
    void eventTypeWithCorrectSubType() throws Exception {
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("swansea");
        courthouse.setDisplayName("swansea1");

        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber("CaseNumber");
        courtCase.setClosed(false);
        courtCase.setInterpreterUsed(false);
        courtCase.setCourthouse(courthouse);
        dartsDatabase.getCaseRepository().save(courtCase);

        String requestBody = """
            {
              "message_id": "useExistingCase",
              "type": "40750",
              "sub_type": "12309",
              "courthouse": "swansea",
              "courtroom": "1",
              "case_numbers": [
                "casenumber"
              ],
              "date_time": "2023-06-14T08:37:30.945Z"
            }""";


        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(requestBody);

        setupExternalUserForCourthouse(courthouse);

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isCreated());

        List<EventEntity> results = dartsDatabase.getAllEvents()
            .stream()
            .filter(eventEntity -> "useExistingCase".equals(eventEntity.getMessageId()))
            .toList();

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("40750", results.get(0).getEventType().getType());
        Assertions.assertEquals("12309", results.get(0).getEventType().getSubType());
    }

    @Test
    void eventTypeWithInvalidSubType() throws Exception {
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("swansea");
        courthouse.setDisplayName("swansea1");

        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber("CaseNumber");
        courtCase.setClosed(false);
        courtCase.setInterpreterUsed(false);
        courtCase.setCourthouse(courthouse);
        dartsDatabase.getCaseRepository().save(courtCase);

        String requestBody = """
            {
              "message_id": "useExistingCase",
              "type": "40750",
              "sub_type": "1002",
              "courthouse": "swansea",
              "courtroom": "1",
              "case_numbers": [
                "casenumber"
              ],
              "date_time": "2023-06-14T08:37:30.945Z"
            }""";


        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT)
            .header("Content-Type", "application/json")
            .content(requestBody);

        setupExternalUserForCourthouse(courthouse);

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isCreated());

        List<EventEntity> results = dartsDatabase.getAllEvents()
            .stream()
            .filter(eventEntity -> "useExistingCase".equals(eventEntity.getMessageId()))
            .toList();

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals("40750", results.get(0).getEventType().getType());
        Assertions.assertNull(results.get(0).getEventType().getSubType());
    }

    private static EventHandlerEntity getActiveHandler() {
        var activeHandler = getHandlerWithDefaults();
        activeHandler.setActive(true);
        activeHandler.setIsReportingRestriction(false);
        return activeHandler;
    }

    private static EventHandlerEntity getInactiveHandler() {
        var inactiveHandler = getHandlerWithDefaults();
        inactiveHandler.setActive(false);
        inactiveHandler.setIsReportingRestriction(false);
        return inactiveHandler;
    }

    private static EventHandlerEntity getHandlerWithDefaults() {
        return createEventHandlerWith("StandardEventHandler", "ActiveTestType", "ActiveTestSubType");
    }

    private void setupExternalUserForCourthouse(CourthouseEntity courthouse) {
        String guid = UUID.randomUUID().toString();
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().createXhibitExternalUser(guid, courthouse);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        when(mockUserIdentity.userHasGlobalAccess(Set.of(XHIBIT, CPP))).thenReturn(true);
    }
}
