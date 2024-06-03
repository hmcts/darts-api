package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.component.AudioRequestBeingProcessedFromArchiveQuery;
import uk.gov.hmcts.darts.audio.model.AudioRequestBeingProcessedFromArchiveQueryResult;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@Transactional
@TestInstance(PER_CLASS)
class AudioRequestBeingProcessedFromArchiveQueryImplIntTest extends IntegrationBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AudioRequestBeingProcessedFromArchiveQuery audioRequestBeingProcessedFromArchiveQuery;

    Integer COURTHOUSE_ID = -1000201;
    Integer USER_ACCOUNT_ID = -1000202;
    Integer SECURITY_USER_ID = -1000203;
    Integer COURT_CASE_ID = -1000204;
    Integer COURTROOM_ID = -1000205;
    Integer HEARING_ID = -1000207;
    Integer MEDIA_ID = -1000208;
    Integer MEDIA_ID2 = -1000210;
    Integer MEDIA_ID3 = -1000211;
    Integer MEDIA_ID4 = -1000212;
    Integer MEDIA_ID5 = -1000213;
    Integer MEDIA_ID6 = -1000214;
    Integer MEDIA_ID7 = -1000215;
    Integer MEDIA_ID8 = -1000216;
    Integer MEDIA_ID9 = -1000217;
    Integer MEDIA_ID10 = -1000218;
    Integer MEDIA_ID11 = -1000219;
    Integer MEDIA_ID12 = -1000220;
    Integer MEDIA_ID13 = -1000221;
    Integer MEDIA_ID14 = -1000222;
    Integer MEDIA_ID15 = -1000223;
    Integer MEDIA_ID16 = -1000224;

    Integer EOD_ID = -1000225;
    Integer EOD_ID1 = -1000226;
    Integer EOD_ID2 = -1000227;
    Integer EOD_ID3 = -1000228;
    Integer EOD_ID4 = -1000229;
    Integer EOD_ID5 = -1000230;
    Integer EOD_ID6 = -1000231;
    Integer EOD_ID7 = -1000232;
    Integer EOD_ID8 = -1000233;
    Integer EOD_ID9 = -1000234;
    Integer EOD_ID10 = -1000235;
    Integer EOD_ID11 = -1000236;
    Integer EOD_ID112= -1000237;

    Integer MEDIA_REQUEST = -1000238;

    @BeforeAll
    @SuppressWarnings("checkstyle:linelength")
    void beforeAll() {
        String courtHouseName = UUID.randomUUID().toString();

        jdbcTemplate.update(
            """
                INSERT INTO darts.courthouse (cth_id, courthouse_code, courthouse_name, created_ts, last_modified_ts, created_by, last_modified_by, display_name)
                VALUES (${COURTHOUSE_ID}, NULL, '${COURTHOUSE_NAME}', '2023-11-17 15:06:15.859244+00', '2023-11-17 15:06:15.859244+00', NULL, NULL, 'Bristol');

                INSERT INTO darts.user_account (usr_id, dm_user_s_object_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_login_ts, last_modified_by, created_by, account_guid, is_system_user, is_active, user_full_name)
                VALUES (${USER_ACCOUNT_ID}, NULL, 'Richard B', 'Richard.B@example.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, true, 'Richard B');

                INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id)
                VALUES (-4, ${COURTHOUSE_ID});

                INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id)
                VALUES (${USER_ACCOUNT_ID}, -4);

                INSERT INTO darts.court_case (cas_id, cth_id, evh_id, case_object_id, case_number, case_closed, interpreter_used, case_closed_ts, created_ts, created_by, last_modified_ts, last_modified_by)
                VALUES (${COURT_CASE_ID}, ${COURTHOUSE_ID}, NULL, NULL, 'T20231009-1', false, false, NULL, NULL, NULL, NULL, NULL);

                INSERT INTO darts.courtroom (ctr_id, cth_id, courtroom_name, created_ts, created_by)
                VALUES (${COURTROOM_ID}, ${COURTHOUSE_ID}, 'Court 1', NULL, NULL);

                INSERT INTO darts.hearing (hea_id, cas_id, ctr_id, hearing_date, scheduled_start_time, hearing_is_actual, judge_hearing_date, created_ts, created_by, last_modified_ts, last_modified_by)
                VALUES (${HEARING_ID}, ${COURT_CASE_ID}, ${COURTROOM_ID}, '2024-01-04', NULL, false, NULL, '2024-01-04 15:52:41.084085+00', NULL, '2024-01-04 15:52:41.084114+00', NULL);

                INSERT INTO darts.media (med_id, ctr_id, media_object_id, channel, total_channels, reference_id, start_ts, end_ts, case_number, created_ts, created_by, last_modified_ts, last_modified_by, media_file, media_format, file_size, checksum, media_type, content_object_id, is_hidden, media_status)
                VALUES
                (${MEDIA_ID}, ${COURTROOM_ID}, NULL, 1, 4, NULL, '2024-01-04 11:00:00+00', '2024-01-04 11:00:05+00', '{T20231009-1}', '2024-01-04 15:52:41.020977+00', NULL, '2024-01-04 15:52:41.090043+00', NULL, '0001.a00', 'mpeg2', 240744, 'wysXTgRikGN6nMB8AJ0JrQ==', 'A', NULL, false, NULL),
                (${MEDIA_ID2}, ${COURTROOM_ID}, NULL, 2, 4, NULL, '2024-01-04 11:00:00+00', '2024-01-04 11:00:05+00', '{T20231009-1}', '2024-01-04 15:55:08.840021+00', NULL, '2024-01-04 15:55:08.85461+00', NULL, '0001.a01', 'mpeg2', 240744, 'wysXTgRikGN6nMB8AJ0JrQ==', 'A', NULL, false, NULL),
                (${MEDIA_ID3}, ${COURTROOM_ID}, NULL, 3, 4, NULL, '2024-01-04 11:00:00+00', '2024-01-04 11:00:05+00', '{T20231009-1}', '2024-01-04 15:58:18.090826+00', NULL, '2024-01-04 15:58:18.123383+00', NULL, '0001.a02', 'mpeg2', 240744, 'wysXTgRikGN6nMB8AJ0JrQ==', 'A', NULL, false, NULL),
                (${MEDIA_ID4}, ${COURTROOM_ID}, NULL, 4, 4, NULL, '2024-01-04 11:00:00+00', '2024-01-04 11:00:05+00', '{T20231009-1}', '2024-01-04 15:59:16.518523+00', NULL, '2024-01-04 15:59:16.543656+00', NULL, '0001.a03', 'mpeg2', 240744, 'wysXTgRikGN6nMB8AJ0JrQ==', 'A', NULL, false, NULL),
                (${MEDIA_ID5}, ${COURTROOM_ID}, NULL, 1, 4, NULL, '2024-01-04 11:00:10+00', '2024-01-04 11:00:15+00', '{T20231009-1}', '2024-01-04 16:02:05.190864+00', NULL, '2024-01-04 16:02:05.205125+00', NULL, '0002.a00', 'mpeg2', 240744, 'T2UrmSYWNZmvvYqBcMnV0g==', 'A', NULL, false, NULL),
                (${MEDIA_ID6}, ${COURTROOM_ID}, NULL, 2, 4, NULL, '2024-01-04 11:00:10+00', '2024-01-04 11:00:15+00', '{T20231009-1}', '2024-01-04 16:04:08.13524+00', NULL, '2024-01-04 16:04:08.164205+00', NULL, '0002.a01', 'mpeg2', 240744, 'T2UrmSYWNZmvvYqBcMnV0g==', 'A', NULL, false, NULL),
                (${MEDIA_ID7}, ${COURTROOM_ID}, NULL, 3, 4, NULL, '2024-01-04 11:00:10+00', '2024-01-04 11:00:15+00', '{T20231009-1}', '2024-01-04 16:05:56.147182+00', NULL, '2024-01-04 16:05:56.167154+00', NULL, '0002.a02', 'mpeg2', 240744, 'T2UrmSYWNZmvvYqBcMnV0g==', 'A', NULL, false, NULL),
                (${MEDIA_ID8}, ${COURTROOM_ID}, NULL, 4, 4, NULL, '2024-01-04 11:00:10+00', '2024-01-04 11:00:15+00', '{T20231009-1}', '2024-01-04 16:06:39.336742+00', NULL, '2024-01-04 16:06:39.351721+00', NULL, '0002.a03', 'mpeg2', 240744, 'T2UrmSYWNZmvvYqBcMnV0g==', 'A', NULL, false, NULL),
                (${MEDIA_ID9}, ${COURTROOM_ID}, NULL, 1, 4, NULL, '2024-01-04 11:00:15+00', '2024-01-04 11:00:20+00', '{T20231009-1}', '2024-01-04 16:10:03.323052+00', NULL, '2024-01-04 16:10:03.340479+00', NULL, '0003.a00', 'mpeg2', 240744, 'DCu19W4toRtk4h5/d76+AQ==', 'A', NULL, false, NULL),
                (${MEDIA_ID10}, ${COURTROOM_ID}, NULL, 2, 4, NULL, '2024-01-04 11:00:15+00', '2024-01-04 11:00:20+00', '{T20231009-1}', '2024-01-04 16:10:47.145318+00', NULL, '2024-01-04 16:10:47.187171+00', NULL, '0003.a01', 'mpeg2', 240744, 'DCu19W4toRtk4h5/d76+AQ==', 'A', NULL, false, NULL),
                (${MEDIA_ID11}, ${COURTROOM_ID}, NULL, 3, 4, NULL, '2024-01-04 11:00:15+00', '2024-01-04 11:00:20+00', '{T20231009-1}', '2024-01-04 16:12:13.419041+00', NULL, '2024-01-04 16:12:13.442933+00', NULL, '0003.a02', 'mpeg2', 240744, 'DCu19W4toRtk4h5/d76+AQ==', 'A', NULL, false, NULL),
                (${MEDIA_ID12}, ${COURTROOM_ID}, NULL, 4, 4, NULL, '2024-01-04 11:00:15+00', '2024-01-04 11:00:20+00', '{T20231009-1}', '2024-01-04 16:12:59.607793+00', NULL, '2024-01-04 16:12:59.640079+00', NULL, '0003.a03', 'mpeg2', 240744, 'DCu19W4toRtk4h5/d76+AQ==', 'A', NULL, false, NULL),
                (${MEDIA_ID13}, ${COURTROOM_ID}, NULL, 1, 4, NULL, '2024-01-04 11:00:30+00', '2024-01-04 11:00:35+00', '{T20231009-1}', '2024-01-04 16:14:36.967939+00', NULL, '2024-01-04 16:14:36.988925+00', NULL, '0004.a00', 'mpeg2', 240744, 'PmpSQZyELyNV4o1HuhF9HA==', 'A', NULL, false, NULL),
                (${MEDIA_ID14}, ${COURTROOM_ID}, NULL, 2, 4, NULL, '2024-01-04 11:00:30+00', '2024-01-04 11:00:35+00', '{T20231009-1}', '2024-01-04 16:15:37.00364+00', NULL, '2024-01-04 16:15:37.034789+00', NULL, '0004.a01', 'mpeg2', 240744, 'PmpSQZyELyNV4o1HuhF9HA==', 'A', NULL, false, NULL),
                (${MEDIA_ID15}, ${COURTROOM_ID}, NULL, 3, 4, NULL, '2024-01-04 11:00:30+00', '2024-01-04 11:00:35+00', '{T20231009-1}', '2024-01-04 16:32:36.089388+00', NULL, '2024-01-04 16:32:36.117606+00', NULL, '0004.a02', 'mpeg2', 240744, 'PmpSQZyELyNV4o1HuhF9HA==', 'A', NULL, false, NULL),
                (${MEDIA_ID16}, ${COURTROOM_ID}, NULL, 4, 4, NULL, '2024-01-04 11:00:30+00', '2024-01-04 11:00:35+00', '{T20231009-1}', '2024-01-04 16:33:09.466088+00', NULL, '2024-01-04 16:33:09.507292+00', NULL, '0004.a03', 'mpeg2', 240744, 'PmpSQZyELyNV4o1HuhF9HA==', 'A', NULL, false, NULL);

                INSERT INTO darts.hearing_media_ae (hea_id, med_id)
                VALUES
                (${HEARING_ID}, ${MEDIA_ID}),
                (${HEARING_ID}, ${MEDIA_ID2}),
                (${HEARING_ID}, ${MEDIA_ID3}),
                (${HEARING_ID}, ${MEDIA_ID4}),
                (${HEARING_ID}, ${MEDIA_ID5}),
                (${HEARING_ID}, ${MEDIA_ID6}),
                (${HEARING_ID}, ${MEDIA_ID7}),
                (${HEARING_ID}, ${MEDIA_ID8}),
                (${HEARING_ID}, ${MEDIA_ID9}),
                (${HEARING_ID}, ${MEDIA_ID10}),
                (${HEARING_ID}, ${MEDIA_ID11}),
                (${HEARING_ID}, ${MEDIA_ID12}),
                (${HEARING_ID}, ${MEDIA_ID13}),
                (${HEARING_ID}, ${MEDIA_ID14}),
                (${HEARING_ID}, ${MEDIA_ID15}),
                (${HEARING_ID}, ${MEDIA_ID16});

                INSERT INTO darts.external_object_directory (eod_id, med_id, trd_id, ado_id, ors_id, elt_id, external_location, checksum, transfer_attempts, created_ts, last_modified_ts, last_modified_by, created_by, cad_id, manifest_file, event_date_ts, external_file_id, external_record_id)
                VALUES
                (${EOD_ID}, ${MEDIA_ID}, NULL, NULL, 2, 1, '197b3953-c3dc-417e-a7c2-a974339b96ae', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:52:41.115953+00', '2024-01-04 15:52:41.11597+00', -45, -45, NULL, NULL, NULL, NULL, NULL),
                (${EOD_ID2}, ${MEDIA_ID2}, NULL, NULL, 2, 1, 'afafc1d0-b1e0-4955-88ea-616a35073e08', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:55:08.852917+00', '2024-01-04 15:55:08.852923+00', -45, -45, NULL, NULL, NULL, NULL, NULL),
                (${EOD_ID3}, ${MEDIA_ID3}, NULL, NULL, 2, 1, 'c15137df-1a2b-4c0d-b309-bc9238330efb', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:58:18.120536+00', '2024-01-04 15:58:18.120547+00', -45, -45, NULL, NULL, NULL, NULL, NULL),
                (${EOD_ID4}, ${MEDIA_ID4}, NULL, NULL, 2, 1, '8f37e682-633c-465b-b1e3-75cdf4a04d85', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:59:16.538801+00', '2024-01-04 15:59:16.538821+00', -45, -45, NULL, NULL, NULL, NULL, NULL),

                (${EOD_ID5}, ${MEDIA_ID4}, NULL, NULL, 11, 2, 'e9dce141-5f58-4bfd-8660-bce8e0759acb', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:59:30.057281+00', '2024-01-04 15:59:30.291683+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (${EOD_ID6}, ${MEDIA_ID}, NULL, NULL, 11, 2, 'e4a003a0-2184-4f2b-a4e2-4ed85a36f734', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:53:30.132639+00', '2024-01-04 15:53:30.486242+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (${EOD_ID7}, ${MEDIA_ID2}, NULL, NULL, 11, 2, 'a3c10ea0-01c2-43f1-bfe7-3b25b7902dfb', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:55:30.075869+00', '2024-01-04 15:55:30.334252+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (${EOD_ID8}, ${MEDIA_ID3}, NULL, NULL, 11, 2, '0e5d566a-00d9-4d02-bcaf-f01f7c582f99', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:58:30.071231+00', '2024-01-04 15:58:30.345809+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (${EOD_ID9}, ${MEDIA_ID4}, NULL, NULL, 2, 3, '8b7dff0f-a2e7-4210-8a5e-f216d8c874eb', 'wysXTgRikGN6nMB8AJ0JrQ==', 1, '2024-01-22 16:10:17.427415+00', '2024-01-22 16:10:17.681265+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (${EOD_ID10}, ${MEDIA_ID}, NULL, NULL, 2, 3, '71c3e02b-b7d2-4603-be74-e8c39faaf285', 'wysXTgRikGN6nMB8AJ0JrQ==', 1, '2024-01-22 16:10:19.834748+00', '2024-01-22 16:10:20.093688+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (${EOD_ID11}, ${MEDIA_ID2}, NULL, NULL, 2, 3, '0dde5ec4-d16d-4940-a923-a73bacd969bb', 'wysXTgRikGN6nMB8AJ0JrQ==', 1, '2024-01-22 16:10:20.882012+00', '2024-01-22 16:10:21.144768+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (${EOD_ID12}, ${MEDIA_ID3}, NULL, NULL, 2, 3, '2ba50586-e892-4ff9-a11a-67c38d23837a', 'wysXTgRikGN6nMB8AJ0JrQ==', 1, '2024-01-22 16:10:21.677894+00', '2024-01-22 16:10:21.933572+00', 0, 0, NULL, NULL, NULL, NULL, NULL);

                INSERT INTO darts.media_request (mer_id, hea_id, requestor, request_status, request_type, req_proc_attempts, start_ts, end_ts, created_ts, last_modified_ts, created_by, last_modified_by, current_owner)
                VALUES
                (${MEDIA_REQUEST}, ${HEARING_ID}, ${USER_ACCOUNT_ID}, 'OPEN', 'DOWNLOAD', 0, '2024-01-04 11:00:02+00', '2024-01-04 11:00:19+00', '2024-01-22 15:41:04.393833+00', '2024-01-22 15:41:04.393866+00', ${USER_ACCOUNT_ID}, ${USER_ACCOUNT_ID}, ${USER_ACCOUNT_ID});
                """
                .replace("${COURTHOUSE_NAME}", courtHouseName)
                .replace("${COURTHOUSE_ID}", COURTHOUSE_ID.toString())
                .replace("${USER_ACCOUNT_ID}", USER_ACCOUNT_ID.toString())
                .replace("${SECURITY_USER_ID}", SECURITY_USER_ID.toString())
                .replace("${COURT_CASE_ID}", COURT_CASE_ID.toString())
                .replace("${COURTROOM_ID}", COURTROOM_ID.toString())
                .replace("${MEDIA_ID}", MEDIA_ID.toString())
                .replace("${MEDIA_ID2}", MEDIA_ID2.toString())
                .replace("${MEDIA_ID3}", MEDIA_ID3.toString())
                .replace("${MEDIA_ID4}", MEDIA_ID4.toString())
                .replace("${MEDIA_ID5}", MEDIA_ID5.toString())
                .replace("${MEDIA_ID6}", MEDIA_ID6.toString())
                .replace("${MEDIA_ID7}", MEDIA_ID7.toString())
                .replace("${MEDIA_ID8}", MEDIA_ID8.toString())
                .replace("${MEDIA_ID9}", MEDIA_ID9.toString())
                .replace("${MEDIA_ID10}", MEDIA_ID10.toString())
                .replace("${MEDIA_ID11}", MEDIA_ID11.toString())
                .replace("${MEDIA_ID12}", MEDIA_ID12.toString())
                .replace("${MEDIA_ID13}", MEDIA_ID13.toString())
                .replace("${MEDIA_ID14}", MEDIA_ID14.toString())
                .replace("${MEDIA_ID15}", MEDIA_ID15.toString())
                .replace("${MEDIA_ID16}", MEDIA_ID16.toString())
                .replace("${EOD_ID}", EOD_ID.toString())
                .replace("${EOD_ID2}", EOD_ID2.toString())
                .replace("${EOD_ID3}", EOD_ID3.toString())
                .replace("${EOD_ID4}", EOD_ID4.toString())
                .replace("${EOD_ID5}", EOD_ID5.toString())
                .replace("${EOD_ID6}", EOD_ID6.toString())
                .replace("${EOD_ID7}", EOD_ID7.toString())
                .replace("${EOD_ID8}", EOD_ID8.toString())
                .replace("${EOD_ID9}", EOD_ID9.toString())
                .replace("${EOD_ID10}", EOD_ID10.toString())
                .replace("${EOD_ID11}", EOD_ID11.toString())
                .replace("${EOD_ID12}", EOD_ID112.toString())
                .replace("${MEDIA_REQUEST}", MEDIA_REQUEST.toString())
                .replace("${HEARING_ID}", HEARING_ID.toString())
        );
    }

    @Test
    void givenAudioRequestBeingProcessedFromArchive_thenReturnResults() throws Exception {
        Integer mediaRequestId = MEDIA_REQUEST;
        final List<AudioRequestBeingProcessedFromArchiveQueryResult> results = audioRequestBeingProcessedFromArchiveQuery.getResults(
            mediaRequestId);

        List expected = List.of(
            new AudioRequestBeingProcessedFromArchiveQueryResult(MEDIA_ID4, EOD_ID5, EOD_ID9),
            new AudioRequestBeingProcessedFromArchiveQueryResult(MEDIA_ID3, EOD_ID8, EOD_ID112),
            new AudioRequestBeingProcessedFromArchiveQueryResult(MEDIA_ID2, EOD_ID7, EOD_ID11),
            new AudioRequestBeingProcessedFromArchiveQueryResult(MEDIA_ID, EOD_ID6, EOD_ID10)
        );
        assertEquals(expected.size(), results.size());
        assertEquals(expected, results);
    }

}