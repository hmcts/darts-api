package uk.gov.hmcts.darts.common.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.AuditActivityEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.stubs.AuditStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@Sql(scripts = "/sql/remove-audit-and-activity.sql", executionPhase = AFTER_TEST_METHOD)
class AuditApiTest {
    public static final int EVENT_ID = 1;
    public static final int CASE_ID = 1;
    public static final int ID = 1;
    private static final int USER_ID = 1;
    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    protected DartsDatabaseStub dartsDatabaseStub;

    @BeforeEach
    public void before() {
        CourtCaseEntity courtCase = dartsDatabaseStub.createCase("TestCourthouse", "TestCourtCase");
        AuditStub auditStub = dartsDatabaseStub.getAuditStub();
        UserAccountStub userAccountStub = dartsDatabaseStub.getUserAccountStub();
        UserAccountEntity defaultUser = userAccountStub.getDefaultUser();
        AuditActivityEntity anyAuditActivity = auditStub.getAnyAuditActivity();

        AuditEntity auditEntity = auditStub.createAuditEntity(
            courtCase,
            anyAuditActivity,
            defaultUser,
            "application_server",
            "additional_data"
        );
        auditEntity.setCreatedDateTime(OffsetDateTime.of(2023, 6, 13, 8, 13, 9, 0, ZoneOffset.UTC));
        dartsDatabaseStub.getAuditRepository().saveAndFlush(auditEntity);

    }

    private static Stream<Arguments> nonExistingId() {
        return Stream.of(
            arguments("case_id", 1111),
            arguments("event_id", 9999)
        );
    }

    private static Stream<Arguments> singleDateFilter() {
        return Stream.of(
            arguments("to_date", "2023-06-13T08:13:09.688537759Z"),
            arguments("from_date", "2023-06-13T08:13:09.688537759Z")
        );
    }

    @Test
    void searchForAuditByIds() throws Exception {
        CourtCaseEntity courtCase = dartsDatabaseStub.createCase("TestCourthouse", "TestCourtCase2");
        AuditStub auditStub = dartsDatabaseStub.getAuditStub();
        UserAccountStub userAccountStub = dartsDatabaseStub.getUserAccountStub();
        UserAccountEntity defaultUser = userAccountStub.getDefaultUser();
        AuditActivityEntity newAuditActivity = auditStub.createTestAuditActivityEntity();
        AuditEntity auditEntity = auditStub.createAuditEntity(
            courtCase,
            newAuditActivity,
            defaultUser,
            "application_server",
            "additional_data"
        );

        //get by caseId
        MockHttpServletRequestBuilder requestBuilder = get("/audit/search")
            .queryParam("case_id", courtCase.getId().toString())
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id", is(auditEntity.getId())))
            .andExpect(jsonPath("$[0].case_id", is(courtCase.getId())))
            .andExpect(jsonPath("$[0].created_at", is(notNullValue())))
            .andExpect(jsonPath("$[0].event_id", is(auditEntity.getAuditActivity())))
            .andExpect(jsonPath("$[0].user_id", is(USER_ID)))
            .andExpect(jsonPath("$[0].application_server", is("application_server")))
            .andExpect(jsonPath("$[0].additional_data", is("additional_data")));

        //get by eventId
        requestBuilder = get("/audit/search")
            .queryParam("event_id", Integer.toString(auditEntity.getAuditActivity()))
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id", is(auditEntity.getId())))
            .andExpect(jsonPath("$[0].case_id", is(courtCase.getId())))
            .andExpect(jsonPath("$[0].created_at", is(notNullValue())))
            .andExpect(jsonPath("$[0].event_id", is(auditEntity.getAuditActivity())))
            .andExpect(jsonPath("$[0].user_id", is(USER_ID)))
            .andExpect(jsonPath("$[0].application_server", is("application_server")))
            .andExpect(jsonPath("$[0].additional_data", is("additional_data")));
    }

    @Test
    void searchForAuditOutsideOfDates() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/audit/search")
            .queryParam("from_date", "2020-01-13T08:13:09.688537759Z")
            .queryParam("to_date", "2020-02-13T08:13:09.688537759Z")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isOk()).andExpect(content().string("[]"));
    }

    @Test
    void searchForAuditBetweenDates() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/audit/search")
            .queryParam("from_date", "2023-05-13T08:13:09.688537759Z")
            .queryParam("to_date", "2023-07-13T08:13:09.688537759Z")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id", is(ID)))
            .andExpect(jsonPath("$[0].created_at", is(notNullValue())))
            .andExpect(jsonPath("$[0].event_id", is(EVENT_ID)))
            .andExpect(jsonPath("$[0].user_id", is(USER_ID)))
            .andExpect(jsonPath("$[0].application_server", is("application_server")))
            .andExpect(jsonPath("$[0].additional_data", is("additional_data")));
    }

    @ParameterizedTest
    @MethodSource("nonExistingId")
    void searchForNonExistingAuditByIds(String idType, int id) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/audit/search")
            .queryParam(idType, Integer.toString(id))
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isOk()).andExpect(content().string("[]"));
    }

    @Test
    void searchForAuditWithWrongParams() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/audit/search")
            .queryParam("wrong_query_param", "value")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title", is("All filters were empty.")));

    }

    @ParameterizedTest
    @MethodSource("singleDateFilter")
    void searchForAuditWithOneDateMissing(String dateFilter, String date) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/audit/search")
            .queryParam(dateFilter, date)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title", is("When using date filters, both must be provided.")));
    }
}
