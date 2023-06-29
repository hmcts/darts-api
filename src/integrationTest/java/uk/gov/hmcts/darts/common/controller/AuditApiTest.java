package uk.gov.hmcts.darts.common.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.util.ClearDatabase;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@ClearDatabase
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert","PMD.AvoidDuplicateLiterals"})
class AuditApiTest {
    public static final int EVENT_ID = 998;
    public static final int CASE_ID = 2;
    public static final int ID = 999;
    private static final int USER_ID = 4;
    @Autowired
    private transient MockMvc mockMvc;

    private static Stream<Arguments> existingIdTypeAndId() {
        return Stream.of(
            arguments("case_id", CASE_ID),
            arguments("event_id", EVENT_ID)
        );
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

    @ParameterizedTest
    @MethodSource("existingIdTypeAndId")
    void searchForAuditByIds(String idType, int id) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/audit/search")
            .queryParam(idType, Integer.toString(id))
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id", is(ID)))
            .andExpect(jsonPath("$[0].case_id", is(CASE_ID)))
            .andExpect(jsonPath("$[0].created_at", is(notNullValue())))
            .andExpect(jsonPath("$[0].event_id", is(EVENT_ID)))
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
            .andExpect(jsonPath("$[0].case_id", is(CASE_ID)))
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
            .andExpect(jsonPath("$.message", is("All filters were empty. ")));
    }

    @ParameterizedTest
    @MethodSource("singleDateFilter")
    void searchForAuditWithOneDateMissing(String dateFilter, String date) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/audit/search")
            .queryParam(dateFilter, date)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("When using date filters, both must be provided. ")));
    }
}
