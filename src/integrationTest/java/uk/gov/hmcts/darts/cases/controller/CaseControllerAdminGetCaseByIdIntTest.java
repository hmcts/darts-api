package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static java.util.stream.IntStream.rangeClosed;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCase;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.createEventWith;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createJudgeWithName;
import static uk.gov.hmcts.darts.test.common.data.ProsecutorTestData.createProsecutorForCase;

@AutoConfigureMockMvc
class CaseControllerAdminGetCaseByIdIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/cases/{id}";

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "1";
    private static final OffsetDateTime DATE_TIME = OffsetDateTime.parse("2023-06-26T13:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;
    @Autowired
    private UserAccountStub accountStub;
    @MockitoBean
    private UserIdentity mockUserIdentity;

    @BeforeEach
    void setupData() {

        CourthouseEntity swanseaCourthouse = someMinimalCourthouse();
        swanseaCourthouse.setCourthouseName("SWANSEA");
        swanseaCourthouse.setDisplayName("SWANSEA");

        CourtCaseEntity case1 = PersistableFactory.getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case1.setCaseNumber("Case1");

        JudgeEntity judge = createJudgeWithName("aJudge");
        CourtroomEntity courtroom1 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom1");
        HearingEntity hearing1a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 20), judge);
        HearingEntity hearing1b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 21), judge);
        HearingEntity hearing1c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 22), judge);

        dartsDatabase.saveAll(hearing1a, hearing1b, hearing1c);
        EventEntity event1 = createEventWith("eventName", "event1", hearing1a, OffsetDateTime.now());
        dartsDatabase.save(event1);
    }

    @Test
    void adminGetCaseById_ShouldReturnForbiddenError() throws Exception {
        // given
        UserAccountEntity accountEntity = accountStub.createJudgeUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(accountEntity);

        // when
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        // then
        mockMvc.perform(requestBuilder).andExpect(status().isForbidden());
    }

    @Test
    void adminGetCaseById_WithCaseOpenAndReportingRestrictions() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "123",
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );

        List<OffsetDateTime> eventDateTimes = new ArrayList<>();
        eventDateTimes.add(DATE_TIME);
        var reportingRestrictions = createEventsWithDifferentTimestamps(eventDateTimes).stream()
            .map(eve -> dartsDatabase.addHandlerToEvent(eve, 54))
            .toList();
        hearingEntity = dartsDatabase.saveEventsForHearing(hearingEntity, reportingRestrictions);

        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.addProsecutor(createProsecutorForCase(courtCase));
        courtCase.addDefendant(createDefendantForCase(courtCase));
        courtCase.addDefence("aDefence");
        courtCase = dartsDatabase.save(courtCase);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, courtCase.getId());

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        // then
        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminGetCaseByIdTest/testCaseOpen/expectedResponse.json");
        expectedResponse = expectedResponse.replace("<CREATED_AT>", courtCase.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
        expectedResponse = expectedResponse.replace("<LAST_MODIFIED_AT>", courtCase.getLastModifiedDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void adminGetCaseById_WithCaseClosedAndReportingRestrictions() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "123",
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );

        List<OffsetDateTime> eventDateTimes = new ArrayList<>();
        eventDateTimes.add(DATE_TIME);
        var reportingRestrictions = createEventsWithDifferentTimestamps(eventDateTimes).stream()
            .map(eve -> dartsDatabase.addHandlerToEvent(eve, 54))
            .toList();
        hearingEntity = dartsDatabase.saveEventsForHearing(hearingEntity, reportingRestrictions);

        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.addProsecutor(createProsecutorForCase(courtCase));
        courtCase.addDefendant(createDefendantForCase(courtCase));
        courtCase.addDefence("aDefence");
        courtCase.setClosed(true);
        courtCase.setCaseClosedTimestamp(DATE_TIME);
        courtCase = dartsDatabase.save(courtCase);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, courtCase.getId());

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        // then
        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminGetCaseByIdTest/testCaseClosed/expectedResponse.json");
        expectedResponse = expectedResponse.replace("<CREATED_AT>", courtCase.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
        expectedResponse = expectedResponse.replace("<LAST_MODIFIED_AT>", courtCase.getLastModifiedDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void adminGetCaseById_IsAnonymised() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "123",
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.setDataAnonymised(true);
        UserAccountEntity anonymisedBy = dartsDatabase.getUserAccountStub().createUser("aUser");
        courtCase.setDataAnonymisedBy(anonymisedBy.getId());
        OffsetDateTime dataAnonymisedTs = OffsetDateTime.parse("2023-01-01T12:00:00Z");
        courtCase.setDataAnonymisedTs(dataAnonymisedTs);
        courtCase.addProsecutor(createProsecutorForCase(courtCase));
        courtCase.addDefendant(createDefendantForCase(courtCase));
        courtCase.addDefence("aDefence");
        courtCase = dartsDatabase.save(courtCase);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, getCaseId("123", SOME_COURTHOUSE));

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        // then
        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminGetCaseByIdTest/testIsAnonymised/expectedResponse.json");
        expectedResponse = expectedResponse
            .replace("<CREATED_AT>", courtCase.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
            .replace("<LAST_MODIFIED_AT>", courtCase.getLastModifiedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
            .replace("<DATA_ANONYMISED_BY>", String.valueOf(anonymisedBy.getId()));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminGetCaseById_IsDeleted() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "123",
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.setDeleted(true);
        UserAccountEntity deletedBy = dartsDatabase.getUserAccountStub().createUser("aUser");
        courtCase.setDeletedBy(deletedBy);
        courtCase.addProsecutor(createProsecutorForCase(courtCase));
        courtCase.addDefendant(createDefendantForCase(courtCase));
        courtCase.addDefence("aDefence");
        courtCase = dartsDatabase.save(courtCase);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, getCaseId("123", SOME_COURTHOUSE));

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        // then
        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminGetCaseByIdTest/expectedResponse_isDeleted.json");
        expectedResponse = expectedResponse
            .replace("<CREATED_AT>", courtCase.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
            .replace("<LAST_MODIFIED_AT>", courtCase.getLastModifiedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
            .replace("<CASE_DELETED_BY>", String.valueOf(deletedBy.getId()));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminGetCaseById_WithRetentionAndAdditionalCaseDetails() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "123",
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );

        List<OffsetDateTime> eventDateTimes = new ArrayList<>();
        eventDateTimes.add(DATE_TIME);
        var reportingRestrictions = createEventsWithDifferentTimestamps(eventDateTimes).stream()
            .map(eve -> dartsDatabase.addHandlerToEvent(eve, 54))
            .toList();
        hearingEntity = dartsDatabase.saveEventsForHearing(hearingEntity, reportingRestrictions);

        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.addProsecutor(createProsecutorForCase(courtCase));
        courtCase.addDefendant(createDefendantForCase(courtCase));
        courtCase.addDefence("aDefence");
        courtCase.setRetentionUpdated(true);
        courtCase.setRetentionRetries(123);
        courtCase.setRetConfScore(RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED);
        courtCase.setRetConfReason(RetentionConfidenceReasonEnum.CASE_CLOSED);
        courtCase.setRetConfUpdatedTs(OffsetDateTime.of(2024, 1, 2, 3, 4, 5, 6, UTC));

        courtCase.setLegacyCaseObjectId("OBJ_ID");
        courtCase.setCaseObjectName("CASE_OBJ_NAME");
        courtCase.setCaseType("CASE_TYPE");
        courtCase.setUploadPriority(123);
        courtCase = dartsDatabase.save(courtCase);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, courtCase.getId());

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        // then
        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminGetCaseByIdTest/expectedResponse_withRetentionAndAdditionalCaseDetails.json");
        expectedResponse = expectedResponse
            .replace("<CREATED_AT>", courtCase.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
            .replace("<LAST_MODIFIED_AT>", courtCase.getLastModifiedDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminGetCaseById_CaseNotFound() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        // when
        mockMvc.perform(get(ENDPOINT_URL, 25))
            .andExpect(status().isNotFound());
    }

    private Integer getCaseId(String caseNumber, String courthouse) {
        return dartsDatabase.createCase(courthouse, caseNumber).getId();
    }

    private List<EventEntity> createEventsWithDifferentTimestamps(List<OffsetDateTime> eventDateTimes) {
        return rangeClosed(1, eventDateTimes.size())
            .mapToObj(index -> {
                var event = dartsDatabase.getEventStub().createDefaultEvent();
                event.setEventText("some-event-text-" + index);
                event.setMessageId("some-message-id-" + index);
                event.setTimestamp(eventDateTimes.get(index - 1));
                return event;
            }).toList();
    }
}
