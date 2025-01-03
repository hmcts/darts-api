package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.testutils.DateHelper.convertSqlDateTimeToLocalDateTime;
import static uk.gov.hmcts.darts.testutils.DateHelper.todaysDateMinusDaysFormattedForSql;

@AutoConfigureMockMvc
class TranscriptionControllerGetTranscriberTranscriptsIntTest extends IntegrationBase {

    private static final URI ENDPOINT_URI = URI.create("/transcriptions/transcriber-view");
    private static final String USER_ID_HEADER = "user_id";
    private static final String ASSIGNED_QUERY_PARAM = "assigned";
    private static final String PLACEHOLDER_URGENCY_ID = "$URGENCY";
    private static final String TODAYS_DATE = "$TODAYS_DATE";
    private static final String MINUS_89_DAYS = "$MINUS_89_DAYS";
    private static final String MINUS_90_DAYS = "$MINUS_90_DAYS";


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

    String todaysDate;
    String dateMinus89Days;
    String dateMinus90Days;

    @BeforeEach
    @SuppressWarnings({"checkstyle.LineLengthCheck"})
    void beforeAll() {
        todaysDate = todaysDateMinusDaysFormattedForSql(0);
        dateMinus89Days = todaysDateMinusDaysFormattedForSql(89);
        dateMinus90Days = todaysDateMinusDaysFormattedForSql(90);
        setupDataWithUrgency();
    }

    @AfterEach
    void afterAll() {
        deleteTranscription();
    }

    @Test
    void givenAssignedQueryParamOnly_thenReturnBadRequest() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .queryParam(ASSIGNED_QUERY_PARAM, TRUE.toString());

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest()).andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        String expectedResponse = """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "Required request header 'user_id' for method parameter type Integer is not present"
            }
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void givenUserIdHeaderOnly_thenReturnBadRequest() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -410
            );

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest()).andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        String expectedResponse = """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "Required request parameter 'assigned' for method parameter type Boolean is not present"
            }
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void givenSomeOtherUserAndTranscriptRequestsViewRequested_thenReturnEmptyArray() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -1
            )
            .queryParam(ASSIGNED_QUERY_PARAM, FALSE.toString());

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = "[]";
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void givenSomeOtherUserAndYourWorkViewRequested_thenReturnEmptyArray() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -1
            )
            .queryParam(ASSIGNED_QUERY_PARAM, TRUE.toString());

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = "[]";
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void givenTranscriberUserAndTranscriptRequestsViewRequested_thenReturnApprovedTranscription() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -410
            )
            .queryParam(ASSIGNED_QUERY_PARAM, FALSE.toString());

        String expectedStateChangeTs = convertSqlDateTimeToLocalDateTime(dateMinus89Days);

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            [
              {
                "transcription_id": 41,
                "case_id": -1,
                "case_number": "T20231009-1",
                "courthouse_name": "Bristol",
                "hearing_date": "2023-11-17",
                "transcription_type": "Specified Times",
                "status": "Approved",
                "requested_ts": "2023-11-23T16:25:55.304517Z",
                "state_change_ts": ":expectedStateChangeTs",
                "is_manual": true,
                "transcription_urgency": {
                  "transcription_urgency_id": 1,
                  "description": "Standard",
                  "priority_order": 999
                }
              }
            ]
            """.replace(":expectedStateChangeTs", expectedStateChangeTs);
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void givenTranscriberUserAndYourWorkViewRequested_thenReturnUnassignedTranscriptionAndDoNotReturnCompletedTranscriptionFromBeforeTodayInactive()
        throws Exception {

        UserAccountEntity userAccountEntity = userAccountRepository.findById(-410).get();
        userAccountEntity.setActive(false);
        userAccountRepository.save(userAccountEntity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -410
            )
            .queryParam(ASSIGNED_QUERY_PARAM, FALSE.toString());

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        Assertions.assertEquals("[]", actualResponse);
    }

    @Test
    void givenTranscriberUserAndTranscriptRequestsViewRequestedWithNoUrgency_thenReturnApprovedTranscription() throws Exception {
        deleteTranscription();
        setupDataWithoutUrgency();

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -410
            )
            .queryParam(ASSIGNED_QUERY_PARAM, FALSE.toString());

        String expectedStateChangeTs = convertSqlDateTimeToLocalDateTime(dateMinus89Days);

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            [
              {
                "transcription_id": 41,
                "case_id": -1,
                "case_number": "T20231009-1",
                "courthouse_name": "Bristol",
                "hearing_date": "2023-11-17",
                "transcription_type": "Specified Times",
                "status": "Approved",
                "requested_ts": "2023-11-23T16:25:55.304517Z",
                "state_change_ts": ":expectedStateChangeTs",
                "is_manual": true
              }
            ]
            """.replace(":expectedStateChangeTs", expectedStateChangeTs);
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    @SuppressWarnings({"checkstyle.LineLengthCheck"})
    void givenTranscriberAndYourWorkViewRequested_thenReturnAssignedAndCompletedTranscriptionTodayAndDoNotReturnCompletedTranscriptionFromBeforeToday()
        throws Exception {
        // This test expects the "Complete" (trs_id=6) transcription (tra_id=101) to be hidden from "Your work > Completed today" view
        // because the workflow_ts is BEFORE TODAY (workflow_ts='2023-11-24 12:53:42.839577+00').
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -410
            )
            .queryParam(ASSIGNED_QUERY_PARAM, TRUE.toString());

        String expectedStateChangeTs = convertSqlDateTimeToLocalDateTime(dateMinus89Days);
        String expectedStateChangeTodayTs = convertSqlDateTimeToLocalDateTime(todaysDate);

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            [
                {
                    "case_id": -1,
                    "case_number": "T20231009-1",
                    "courthouse_name": "Bristol",
                    "hearing_date": "2023-11-17",
                    "is_manual": true,
                    "requested_ts": "2023-11-24T12:37:34.976469Z",
                    "state_change_ts": ":expectedStateChangeTodayTs",
                    "status": "Complete",
                    "transcription_id": 121,
                    "transcription_type": "Specified Times",
                    "transcription_urgency": {
                        "description": "Standard",
                        "priority_order": 999,
                        "transcription_urgency_id": 1
                    }
                },
                {
                    "case_id": -1,
                    "case_number": "T20231009-1",
                    "courthouse_name": "Bristol",
                    "hearing_date": "2023-11-17",
                    "is_manual": true,
                    "requested_ts": ":expectedStateChangeTs",
                    "state_change_ts": ":expectedStateChangeTs",
                    "status": "With Transcriber",
                    "transcription_id": 81,
                    "transcription_type": "Specified Times",
                    "transcription_urgency": {
                        "description": "Standard",
                        "priority_order": 999,
                        "transcription_urgency_id": 1
                    }
                }
            ]
            """.replace(":expectedStateChangeTs", expectedStateChangeTs)
            .replace(":expectedStateChangeTodayTs", expectedStateChangeTodayTs);
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void givenTranscriberAndYourWorkViewRequestedWithAnInactiveUser_thenReturn()
        throws Exception {

        UserAccountEntity userAccountEntity = userAccountRepository.findById(-410).get();
        userAccountEntity.setActive(false);
        userAccountRepository.save(userAccountEntity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -410
            )
            .queryParam(ASSIGNED_QUERY_PARAM, FALSE.toString());

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        Assertions.assertEquals("[]", actualResponse);
    }

    private void setupDataWithUrgency() {
        setupData(true);
    }

    private void setupDataWithoutUrgency() {
        setupData(false);
    }

    private void setupData(boolean generatedUrgency) {
        jdbcTemplate.update("""
                                INSERT INTO darts.courthouse (cth_id, courthouse_code, courthouse_name, created_ts, last_modified_ts, created_by,
                                last_modified_by, display_name)
                                VALUES (-1, NULL, 'BRISTOL', '2023-11-17 15:06:15.859244+00', '2023-11-17 15:06:15.859244+00', 0, 0, 'Bristol');
                                INSERT INTO darts.courtroom (ctr_id, cth_id, courtroom_name, created_ts, created_by)
                                VALUES (-1, -1, 'COURT 1', current_timestamp, 0);
                                INSERT INTO darts.court_case (cas_id, cth_id, evh_id, case_object_id, case_number, case_closed, interpreter_used,
                                case_closed_ts, created_ts, created_by, last_modified_ts, last_modified_by)
                                VALUES (-1, -1, NULL, NULL, 'T20231009-1', false, false, NULL, current_timestamp, 0, current_timestamp, 0);
                                INSERT INTO darts.hearing (hea_id, cas_id, ctr_id, hearing_date, scheduled_start_time, hearing_is_actual,
                                created_ts, created_by, last_modified_ts, last_modified_by)
                                VALUES (-1, -1, -1, '2023-11-17', NULL, true, current_timestamp, 0, current_timestamp, 0);
                                
                                INSERT INTO darts.user_account (usr_id, dm_user_s_object_id, user_full_name, user_email_address, description,
                                is_active, created_ts,
                                last_modified_ts, last_login_ts, last_modified_by, created_by, account_guid, is_system_user)
                                VALUES (-410, NULL, 'Richard B', 'Richard.B@example.com', NULL, true, current_timestamp,
                                current_timestamp, NULL, 0, 0, NULL, false);
                                INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id)
                                VALUES (-410, -4);
                                INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id)
                                VALUES (-4, -1);

                                -- Transcript Requests: Approved (after status rollback from WITH_TRANSCRIBER)
                                INSERT INTO darts.transcription (tra_id, ctr_id, trt_id, transcription_object_id, requested_by, start_ts, end_ts,
                                created_ts, last_modified_ts, last_modified_by, created_by, tru_id, trs_id, hearing_date,
                                is_manual_transcription, hide_request_from_requestor, is_current)
                                VALUES (41, NULL, 9, NULL, NULL, '2023-11-23 09:00:00+00', '2023-11-23 09:30:00+00', '2023-11-23 16:25:55.297666+00',
                                '2023-11-23 16:26:20.451054+00', -410, -410, $URGENCY, 3, NULL, true, false, true);
                                INSERT INTO darts.case_transcription_ae (tra_id, cas_id) VALUES (41,-1);
                                INSERT INTO darts.hearing_transcription_ae (tra_id, hea_id) VALUES (41,-1);
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (41, 41, 1, -410, '2023-11-23 16:25:55.304517+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (42, 41, 2, -410, '2023-11-23 16:25:55.338405+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (43, 41, 3, -410, '2023-11-23 16:26:20.441633+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (44, 41, 5, -410, '2023-11-23 16:30:00.0+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (45, 41, 3, -410, '$MINUS_89_DAYS');
                                
                                -- Add transcript request to test transcriptions do not show after 90 days
                                INSERT INTO darts.transcription (tra_id, ctr_id, trt_id, transcription_object_id, requested_by, start_ts, end_ts,
                                created_ts, last_modified_ts, last_modified_by, created_by, tru_id, trs_id, hearing_date,
                                is_manual_transcription, hide_request_from_requestor, is_current)
                                VALUES (602, NULL, 9, NULL, NULL, '2023-11-23 09:00:00+00', '2023-11-23 09:30:00+00', '2023-11-23 16:25:55.297666+00',
                                '2023-11-23 16:26:20.451054+00', -410, -410, $URGENCY, 3, NULL, true, false, true);
                                INSERT INTO darts.case_transcription_ae (tra_id, cas_id) VALUES (602,-1);
                                INSERT INTO darts.hearing_transcription_ae (tra_id, hea_id) VALUES (602,-1);
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (100, 602, 1, -410, '$MINUS_90_DAYS');

                                -- Your work > To do: With Transcriber
                                INSERT INTO darts.transcription (tra_id, ctr_id, trt_id, transcription_object_id, requested_by, start_ts, end_ts,
                                created_ts, last_modified_ts, last_modified_by, created_by, tru_id, trs_id, hearing_date,
                                is_manual_transcription, hide_request_from_requestor, is_current)
                                VALUES (81, NULL, 9, NULL, NULL, '2023-11-23 09:20:00+00', '2023-11-23 09:30:00+00', '2023-11-23 17:45:14.938855+00',
                                '2023-11-23 17:45:51.1549+00', -410, -410, $URGENCY, 5, NULL, true, false, true);
                                INSERT INTO darts.case_transcription_ae (tra_id, cas_id) VALUES (81,-1);
                                INSERT INTO darts.hearing_transcription_ae (tra_id, hea_id) VALUES (81,-1);
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (81, 81, 1, -410, '$MINUS_89_DAYS');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (82, 81, 2, -410, '$MINUS_89_DAYS');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (101, 81, 3, -410, '$MINUS_89_DAYS');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (102, 81, 5, -410, '$MINUS_89_DAYS');
                                                                
                                -- Add additional Your work transcription to test transcriptions do not show after 90 days
                                INSERT INTO darts.transcription (tra_id, ctr_id, trt_id, transcription_object_id, requested_by, start_ts, end_ts,
                                created_ts, last_modified_ts, last_modified_by, created_by, tru_id, trs_id, hearing_date,
                                is_manual_transcription, hide_request_from_requestor, is_current)
                                VALUES (601, NULL, 9, NULL, NULL, '2023-11-23 09:20:00+00', '2024-07-01 09:30:00+00', '2024-07-01 17:45:14.938855+00',
                                '2024-07-01 17:45:51.1549+00', -410, -410, $URGENCY, 5, NULL, true, false, true);
                                INSERT INTO darts.case_transcription_ae (tra_id, cas_id) VALUES (601,-1);
                                INSERT INTO darts.hearing_transcription_ae (tra_id, hea_id) VALUES (601,-1);
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (103, 601, 1, -410, '$MINUS_90_DAYS');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (104, 601, 2, -410, '$MINUS_90_DAYS');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (105, 601, 3, -410, '$MINUS_90_DAYS');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (106, 601, 5, -410, '$MINUS_90_DAYS');
                                                                
                                -- This transcription would be hidden from Your work > Completed today (transcriber-view?assigned=true)
                                INSERT INTO darts.transcription (tra_id, ctr_id, trt_id, transcription_object_id, requested_by, start_ts, end_ts,
                                created_ts, last_modified_ts, last_modified_by, created_by, tru_id, trs_id, hearing_date,
                                is_manual_transcription, hide_request_from_requestor, is_current)
                                VALUES (101, NULL, 9, NULL, NULL, '2023-11-24 09:00:00+00', '2023-11-24 09:30:00+00', '2023-11-24 12:37:00.782036+00',
                                '2023-11-24 12:53:42.870475+00', -410, -410, $URGENCY, 6, NULL, true, false, true);
                                INSERT INTO darts.case_transcription_ae (tra_id, cas_id) VALUES (101,-1);
                                INSERT INTO darts.hearing_transcription_ae (tra_id, hea_id) VALUES (101,-1);
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (121, 101, 1, -410, '2023-11-24 12:37:00.812692+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (122, 101, 2, -410, '2023-11-24 12:37:00.846763+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (123, 101, 3, -410, '2023-11-24 12:37:18.762383+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (124, 101, 5, -410, '2023-11-24 12:37:34.976469+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (141, 101, 6, -410, '2023-11-24 12:53:42.839577+00');

                                -- Your work > Completed today: Complete
                                INSERT INTO darts.transcription (tra_id, ctr_id, trt_id, transcription_object_id, requested_by, start_ts, end_ts,
                                created_ts, last_modified_ts, last_modified_by, created_by, tru_id, trs_id, hearing_date,
                                is_manual_transcription, hide_request_from_requestor, is_current)
                                VALUES (121, NULL, 9, NULL, NULL, '$TODAYS_DATE', '$TODAYS_DATE', '$TODAYS_DATE', '$TODAYS_DATE', -410,
                                -410, $URGENCY, 6, NULL, true, false, true);
                                INSERT INTO darts.case_transcription_ae (tra_id, cas_id) VALUES (121,-1);
                                INSERT INTO darts.hearing_transcription_ae (tra_id, hea_id) VALUES (121,-1);
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (161, 121, 1, -410, '2023-11-24 12:37:34.976469+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (162, 121, 2, -410, '$TODAYS_DATE');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (163, 121, 3, -410, '$TODAYS_DATE');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (164, 121, 5, -410, '$TODAYS_DATE');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (165, 121, 6, -410, '$TODAYS_DATE');
                                                                
                                -- test is_current false is not returned
                                INSERT INTO darts.transcription (tra_id, ctr_id, trt_id, transcription_object_id, requested_by, start_ts, end_ts,
                                created_ts, last_modified_ts, last_modified_by, created_by, tru_id, trs_id, hearing_date,
                                is_manual_transcription, hide_request_from_requestor, is_current)
                                VALUES (150, NULL, 9, NULL, NULL, '2023-11-23 09:00:00+00', '2023-11-23 09:30:00+00', '2023-11-23 16:25:55.297666+00',
                                '2023-11-23 16:26:20.451054+00', -410, -410, $URGENCY, 3, NULL, true, false, false);
                                INSERT INTO darts.case_transcription_ae (tra_id, cas_id) VALUES (150,-1);
                                INSERT INTO darts.hearing_transcription_ae (tra_id, hea_id) VALUES (150,-1);
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (241, 150, 1, -410, '2023-11-23 16:25:55.304517+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (242, 150, 2, -410, '2023-11-23 16:25:55.338405+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (243, 150, 3, -410, '2023-11-23 16:26:20.441633+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (244, 150, 5, -410, '2023-11-23 16:30:00.0+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (245, 150, 3, -410, '$MINUS_89_DAYS');
                                                                
                                INSERT INTO darts.transcription (tra_id, ctr_id, trt_id, transcription_object_id, requested_by, start_ts, end_ts,
                                created_ts, last_modified_ts, last_modified_by, created_by, tru_id, trs_id, hearing_date,
                                is_manual_transcription, hide_request_from_requestor, is_current)
                                VALUES (151, NULL, 9, NULL, NULL, '2023-11-23 09:20:00+00', '2023-11-23 09:30:00+00', '2023-11-23 17:45:14.938855+00',
                                '2023-11-23 17:45:51.1549+00', -410, -410, $URGENCY, 5, NULL, true, false, false);
                                INSERT INTO darts.case_transcription_ae (tra_id, cas_id) VALUES (151,-1);
                                INSERT INTO darts.hearing_transcription_ae (tra_id, hea_id) VALUES (151,-1);
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (246, 151, 1, -410, '$MINUS_89_DAYS');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (247, 151, 2, -410, '$MINUS_89_DAYS');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (248, 151, 3, -410, '$MINUS_89_DAYS');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (249, 151, 5, -410, '$MINUS_89_DAYS');
                                                                
                                """.replace(PLACEHOLDER_URGENCY_ID, generatedUrgency ? "1" : "NULL")
                                .replace(TODAYS_DATE, todaysDate)
                                .replace(MINUS_89_DAYS, dateMinus89Days)
                                .replace(MINUS_90_DAYS, dateMinus90Days)
        );
    }

    private void deleteTranscription() {
        jdbcTemplate.update("""
                                DELETE FROM darts.case_transcription_ae WHERE tra_id IN (41, 81, 101, 121, 150, 151, 601, 602);
                                DELETE FROM darts.hearing_transcription_ae WHERE tra_id IN (41, 81, 101, 121, 150, 151, 601, 602);

                                DELETE FROM darts.transcription_workflow WHERE tra_id IN (41, 81, 101, 121, 150, 151, 601, 602);
                                DELETE FROM darts.transcription WHERE tra_id IN (41, 81, 101, 121, 150, 151, 601, 602);

                                DELETE FROM darts.security_group_courthouse_ae WHERE grp_id=-4 AND cth_id=-1;
                                DELETE FROM darts.security_group_user_account_ae WHERE usr_id=-410 AND grp_id=-4;
                                DELETE FROM darts.user_account WHERE usr_id=-410;

                                DELETE FROM darts.hearing WHERE hea_id=-1;
                                DELETE FROM darts.court_case WHERE cas_id=-1;
                                DELETE FROM darts.courtroom WHERE ctr_id=-1;
                                DELETE FROM darts.courthouse WHERE cth_id=-1;
                                """);
    }
}