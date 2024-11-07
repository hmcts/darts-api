package uk.gov.hmcts.darts.transcriptions.controller;

import org.junit.jupiter.api.AfterEach;
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
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;

import java.net.URI;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TranscriptionControllerGetTranscriptionTranscriberCountsIntTest extends IntegrationBase {

    private static final URI ENDPOINT_URI = URI.create("/transcriptions/transcriber-counts");
    private static final String USER_ID_HEADER = "user_id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    @SuppressWarnings({"checkstyle.LineLengthCheck"})
    void beforeEach() {
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
                                
                                INSERT INTO darts.user_account (usr_id, dm_user_s_object_id, user_name, user_full_name, user_email_address, description,
                                is_active, created_ts,
                                last_modified_ts, last_login_ts, last_modified_by, created_by, account_guid, is_system_user)
                                VALUES (-410, NULL, 'John R', 'John R', 'John.R@example.com', NULL, true, current_timestamp,
                                current_timestamp, NULL, 0, 0, NULL, false);
                                INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id)
                                VALUES (-410, -4);
                                INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id)
                                VALUES (-4, -1);
                                
                                INSERT INTO darts.user_account (usr_id, dm_user_s_object_id, user_name, user_full_name, user_email_address, description,
                                is_active, created_ts,
                                last_modified_ts, last_login_ts, last_modified_by, created_by, account_guid, is_system_user)
                                VALUES (-420, NULL, 'John R', 'John R', 'John.R@example.com', NULL, true, current_timestamp,
                                current_timestamp, NULL, 0, 0, NULL, false);
                                
                                -- Transcript Requests: Approved
                                INSERT INTO darts.transcription (tra_id, ctr_id, trt_id, transcription_object_id, requested_by, start_ts, end_ts,
                                created_ts, last_modified_ts, last_modified_by, created_by, tru_id, trs_id, hearing_date,
                                is_manual_transcription, hide_request_from_requestor)
                                VALUES (41, -1, 9, NULL, NULL, '2023-11-23 09:00:00+00', '2023-11-23 09:30:00+00', '2023-11-23 16:25:55.297666+00',
                                '2023-11-23 16:26:20.451054+00', -410, -410, 1, 3, NULL, true, false);
                                INSERT INTO darts.case_transcription_ae (tra_id, cas_id) VALUES (41,-1);
                                INSERT INTO darts.hearing_transcription_ae (tra_id, hea_id) VALUES (41,-1);
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (41, 41, 1, -410, '2023-11-23 16:25:55.304517+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (42, 41, 2, -410, '2023-11-23 16:25:55.338405+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (43, 41, 3, -410, '2023-11-23 16:26:20.441633+00');
                                
                                -- Transcript Requests: With Transcriber
                                INSERT INTO darts.transcription (tra_id, ctr_id, trt_id, transcription_object_id, requested_by, start_ts, end_ts,
                                created_ts, last_modified_ts, last_modified_by, created_by, tru_id, trs_id, hearing_date,
                                is_manual_transcription, hide_request_from_requestor)
                                VALUES (81, NULL, 9, NULL, NULL, '2023-11-23 09:20:00+00', '2023-11-23 09:30:00+00', '2023-11-23 17:45:14.938855+00',
                                '2023-11-23 17:45:51.1549+00', -410, -410, 1, 5, NULL, true, false);
                                INSERT INTO darts.case_transcription_ae (tra_id, cas_id) VALUES (81,-1);
                                INSERT INTO darts.hearing_transcription_ae (tra_id, hea_id) VALUES (81,-1);
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (81, 81, 1, -410, '2023-11-23 17:45:14.940936+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (82, 81, 2, -410, '2023-11-23 17:45:14.948701+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (101, 81, 3, -410, '2023-11-23 17:45:27.069655+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (102, 81, 5, -410, '2023-11-23 17:45:51.151621+00');
                                
                                -- Transcript Requests: Approved
                                INSERT INTO darts.transcription (tra_id, ctr_id, trt_id, transcription_object_id, requested_by, start_ts, end_ts,
                                created_ts, last_modified_ts, last_modified_by, created_by, tru_id, trs_id, hearing_date,
                                is_manual_transcription, hide_request_from_requestor)
                                VALUES (101, NULL, 9, NULL, NULL, '2023-11-24 09:00:00+00', '2023-11-24 09:30:00+00', '2023-11-24 12:37:00.782036+00',
                                '2023-11-24 12:53:42.870475+00', -410, -410, 1, 3, NULL, true, false);
                                INSERT INTO darts.case_transcription_ae (tra_id, cas_id) VALUES (101,-1);
                                INSERT INTO darts.hearing_transcription_ae (tra_id, hea_id) VALUES (101,-1);
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (121, 101, 1, -410, '2023-11-24 12:37:00.812692+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (122, 101, 2, -410, '2023-11-24 12:37:00.846763+00');
                                INSERT INTO darts.transcription_workflow (trw_id, tra_id, trs_id, workflow_actor, workflow_ts)
                                VALUES (123, 101, 3, -410, '2023-11-24 12:37:18.762383+00');
                                """);
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("""
                                DELETE FROM darts.case_transcription_ae WHERE tra_id IN (41, 81, 101);
                                DELETE FROM darts.hearing_transcription_ae WHERE tra_id IN (41, 81, 101);
                                
                                DELETE FROM darts.transcription_workflow WHERE tra_id IN (41, 81, 101);
                                DELETE FROM darts.transcription WHERE tra_id IN (41, 81, 101);
                                
                                DELETE FROM darts.security_group_courthouse_ae WHERE grp_id=-4 AND cth_id=-1;
                                DELETE FROM darts.security_group_user_account_ae WHERE usr_id=-410 AND grp_id=-4;
                                DELETE FROM darts.user_account WHERE usr_id=-410;
                                DELETE FROM darts.user_account WHERE usr_id=-420;
                                
                                DELETE FROM darts.hearing WHERE hea_id=-1;
                                DELETE FROM darts.court_case WHERE cas_id=-1;
                                DELETE FROM darts.courtroom WHERE ctr_id=-1;
                                DELETE FROM darts.courthouse WHERE cth_id=-1;
                                """);
    }


    @Test
    void getTranscriberCountsShouldReturnOk() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -410
            );

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
                "unassigned":2,
                "assigned":1
            }
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriberCountsShouldReturnOkWithInactive() throws Exception {

        UserAccountEntity userAccountEntity = userAccountRepository.findById(-410).get();
        userAccountEntity.setActive(false);
        userAccountRepository.save(userAccountEntity);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -410
            );

        mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.type").value(TranscriptionApiError.USER_NOT_TRANSCRIBER.getType()))
            .andReturn();
    }

    @Test
    void getTranscriberCountsShouldReturnForbiddenWhenUserNotTranscriber() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URI)
            .header(
                USER_ID_HEADER,
                -420
            );

        final MvcResult mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
                "type":"USER_NOT_TRANSCRIBER_113",
                "title":"User is not a transcriber user",
                "status":403
            }
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

}