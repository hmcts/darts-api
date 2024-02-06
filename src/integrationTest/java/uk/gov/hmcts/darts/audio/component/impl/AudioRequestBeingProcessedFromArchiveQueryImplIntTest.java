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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@Transactional
@TestInstance(PER_CLASS)
class AudioRequestBeingProcessedFromArchiveQueryImplIntTest extends IntegrationBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AudioRequestBeingProcessedFromArchiveQuery audioRequestBeingProcessedFromArchiveQuery;

    @BeforeAll
    @SuppressWarnings("checkstyle:linelength")
    void beforeAll() {
        jdbcTemplate.update(
            """
                INSERT INTO darts.courthouse (cth_id, courthouse_code, courthouse_name, created_ts, last_modified_ts, created_by, last_modified_by, display_name)
                VALUES (-1, NULL, 'Bristol', '2023-11-17 15:06:15.859244+00', '2023-11-17 15:06:15.859244+00', NULL, NULL, 'Bristol');

                INSERT INTO darts.user_account (usr_id, dm_user_s_object_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_login_ts, last_modified_by, created_by, account_guid, is_system_user, is_active, user_full_name)
                VALUES (-10, NULL, 'Richard B', 'Richard.B@example.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, true, 'Richard B');

                INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id)
                VALUES (-4, -1);

                INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id)
                VALUES (-10, -4);

                INSERT INTO darts.court_case (cas_id, cth_id, evh_id, case_object_id, case_number, case_closed, interpreter_used, case_closed_ts, version_label, created_ts, created_by, last_modified_ts, last_modified_by)
                VALUES (-1, -1, NULL, NULL, 'T20231009-1', false, false, NULL, NULL, NULL, NULL, NULL, NULL);

                INSERT INTO darts.courtroom (ctr_id, cth_id, courtroom_name, created_ts, created_by)
                VALUES (-1, -1, 'Court 1', NULL, NULL);

                INSERT INTO darts.hearing (hea_id, cas_id, ctr_id, hearing_date, scheduled_start_time, hearing_is_actual, judge_hearing_date, created_ts, created_by, last_modified_ts, last_modified_by)
                VALUES (101, -1, -1, '2024-01-04', NULL, false, NULL, '2024-01-04 15:52:41.084085+00', NULL, '2024-01-04 15:52:41.084114+00', NULL);

                INSERT INTO darts.media (med_id, ctr_id, media_object_id, channel, total_channels, reference_id, start_ts, end_ts, case_number, version_label, created_ts, created_by, last_modified_ts, last_modified_by, media_file, media_format, file_size, checksum, media_type, content_object_id, is_hidden, media_status)
                VALUES
                (181, -1, NULL, 1, 4, NULL, '2024-01-04 11:00:00+00', '2024-01-04 11:00:05+00', '{T20231009-1}', NULL, '2024-01-04 15:52:41.020977+00', NULL, '2024-01-04 15:52:41.090043+00', NULL, '0001.a00', 'mpeg2', 240744, 'wysXTgRikGN6nMB8AJ0JrQ==', 'A', NULL, false, NULL),
                (182, -1, NULL, 2, 4, NULL, '2024-01-04 11:00:00+00', '2024-01-04 11:00:05+00', '{T20231009-1}', NULL, '2024-01-04 15:55:08.840021+00', NULL, '2024-01-04 15:55:08.85461+00', NULL, '0001.a01', 'mpeg2', 240744, 'wysXTgRikGN6nMB8AJ0JrQ==', 'A', NULL, false, NULL),
                (183, -1, NULL, 3, 4, NULL, '2024-01-04 11:00:00+00', '2024-01-04 11:00:05+00', '{T20231009-1}', NULL, '2024-01-04 15:58:18.090826+00', NULL, '2024-01-04 15:58:18.123383+00', NULL, '0001.a02', 'mpeg2', 240744, 'wysXTgRikGN6nMB8AJ0JrQ==', 'A', NULL, false, NULL),
                (184, -1, NULL, 4, 4, NULL, '2024-01-04 11:00:00+00', '2024-01-04 11:00:05+00', '{T20231009-1}', NULL, '2024-01-04 15:59:16.518523+00', NULL, '2024-01-04 15:59:16.543656+00', NULL, '0001.a03', 'mpeg2', 240744, 'wysXTgRikGN6nMB8AJ0JrQ==', 'A', NULL, false, NULL),
                (185, -1, NULL, 1, 4, NULL, '2024-01-04 11:00:10+00', '2024-01-04 11:00:15+00', '{T20231009-1}', NULL, '2024-01-04 16:02:05.190864+00', NULL, '2024-01-04 16:02:05.205125+00', NULL, '0002.a00', 'mpeg2', 240744, 'T2UrmSYWNZmvvYqBcMnV0g==', 'A', NULL, false, NULL),
                (186, -1, NULL, 2, 4, NULL, '2024-01-04 11:00:10+00', '2024-01-04 11:00:15+00', '{T20231009-1}', NULL, '2024-01-04 16:04:08.13524+00', NULL, '2024-01-04 16:04:08.164205+00', NULL, '0002.a01', 'mpeg2', 240744, 'T2UrmSYWNZmvvYqBcMnV0g==', 'A', NULL, false, NULL),
                (187, -1, NULL, 3, 4, NULL, '2024-01-04 11:00:10+00', '2024-01-04 11:00:15+00', '{T20231009-1}', NULL, '2024-01-04 16:05:56.147182+00', NULL, '2024-01-04 16:05:56.167154+00', NULL, '0002.a02', 'mpeg2', 240744, 'T2UrmSYWNZmvvYqBcMnV0g==', 'A', NULL, false, NULL),
                (188, -1, NULL, 4, 4, NULL, '2024-01-04 11:00:10+00', '2024-01-04 11:00:15+00', '{T20231009-1}', NULL, '2024-01-04 16:06:39.336742+00', NULL, '2024-01-04 16:06:39.351721+00', NULL, '0002.a03', 'mpeg2', 240744, 'T2UrmSYWNZmvvYqBcMnV0g==', 'A', NULL, false, NULL),
                (189, -1, NULL, 1, 4, NULL, '2024-01-04 11:00:15+00', '2024-01-04 11:00:20+00', '{T20231009-1}', NULL, '2024-01-04 16:10:03.323052+00', NULL, '2024-01-04 16:10:03.340479+00', NULL, '0003.a00', 'mpeg2', 240744, 'DCu19W4toRtk4h5/d76+AQ==', 'A', NULL, false, NULL),
                (190, -1, NULL, 2, 4, NULL, '2024-01-04 11:00:15+00', '2024-01-04 11:00:20+00', '{T20231009-1}', NULL, '2024-01-04 16:10:47.145318+00', NULL, '2024-01-04 16:10:47.187171+00', NULL, '0003.a01', 'mpeg2', 240744, 'DCu19W4toRtk4h5/d76+AQ==', 'A', NULL, false, NULL),
                (191, -1, NULL, 3, 4, NULL, '2024-01-04 11:00:15+00', '2024-01-04 11:00:20+00', '{T20231009-1}', NULL, '2024-01-04 16:12:13.419041+00', NULL, '2024-01-04 16:12:13.442933+00', NULL, '0003.a02', 'mpeg2', 240744, 'DCu19W4toRtk4h5/d76+AQ==', 'A', NULL, false, NULL),
                (192, -1, NULL, 4, 4, NULL, '2024-01-04 11:00:15+00', '2024-01-04 11:00:20+00', '{T20231009-1}', NULL, '2024-01-04 16:12:59.607793+00', NULL, '2024-01-04 16:12:59.640079+00', NULL, '0003.a03', 'mpeg2', 240744, 'DCu19W4toRtk4h5/d76+AQ==', 'A', NULL, false, NULL),
                (193, -1, NULL, 1, 4, NULL, '2024-01-04 11:00:30+00', '2024-01-04 11:00:35+00', '{T20231009-1}', NULL, '2024-01-04 16:14:36.967939+00', NULL, '2024-01-04 16:14:36.988925+00', NULL, '0004.a00', 'mpeg2', 240744, 'PmpSQZyELyNV4o1HuhF9HA==', 'A', NULL, false, NULL),
                (194, -1, NULL, 2, 4, NULL, '2024-01-04 11:00:30+00', '2024-01-04 11:00:35+00', '{T20231009-1}', NULL, '2024-01-04 16:15:37.00364+00', NULL, '2024-01-04 16:15:37.034789+00', NULL, '0004.a01', 'mpeg2', 240744, 'PmpSQZyELyNV4o1HuhF9HA==', 'A', NULL, false, NULL),
                (201, -1, NULL, 3, 4, NULL, '2024-01-04 11:00:30+00', '2024-01-04 11:00:35+00', '{T20231009-1}', NULL, '2024-01-04 16:32:36.089388+00', NULL, '2024-01-04 16:32:36.117606+00', NULL, '0004.a02', 'mpeg2', 240744, 'PmpSQZyELyNV4o1HuhF9HA==', 'A', NULL, false, NULL),
                (202, -1, NULL, 4, 4, NULL, '2024-01-04 11:00:30+00', '2024-01-04 11:00:35+00', '{T20231009-1}', NULL, '2024-01-04 16:33:09.466088+00', NULL, '2024-01-04 16:33:09.507292+00', NULL, '0004.a03', 'mpeg2', 240744, 'PmpSQZyELyNV4o1HuhF9HA==', 'A', NULL, false, NULL);

                INSERT INTO darts.hearing_media_ae (hea_id, med_id)
                VALUES
                (101, 181),
                (101, 182),
                (101, 183),
                (101, 184),
                (101, 185),
                (101, 186),
                (101, 187),
                (101, 188),
                (101, 189),
                (101, 190),
                (101, 191),
                (101, 192),
                (101, 193),
                (101, 194),
                (101, 201),
                (101, 202);

                INSERT INTO darts.external_object_directory (eod_id, med_id, trd_id, ado_id, ors_id, elt_id, external_location, checksum, transfer_attempts, created_ts, last_modified_ts, last_modified_by, created_by, cad_id, manifest_file, event_date_ts, external_file_id, external_record_id)
                VALUES
                (2541, 181, NULL, NULL, 2, 1, '197b3953-c3dc-417e-a7c2-a974339b96ae', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:52:41.115953+00', '2024-01-04 15:52:41.11597+00', -45, -45, NULL, NULL, NULL, NULL, NULL),
                (2543, 182, NULL, NULL, 2, 1, 'afafc1d0-b1e0-4955-88ea-616a35073e08', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:55:08.852917+00', '2024-01-04 15:55:08.852923+00', -45, -45, NULL, NULL, NULL, NULL, NULL),
                (2544, 183, NULL, NULL, 2, 1, 'c15137df-1a2b-4c0d-b309-bc9238330efb', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:58:18.120536+00', '2024-01-04 15:58:18.120547+00', -45, -45, NULL, NULL, NULL, NULL, NULL),
                (2546, 184, NULL, NULL, 2, 1, '8f37e682-633c-465b-b1e3-75cdf4a04d85', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:59:16.538801+00', '2024-01-04 15:59:16.538821+00', -45, -45, NULL, NULL, NULL, NULL, NULL),

                (2547, 184, NULL, NULL, 11, 2, 'e9dce141-5f58-4bfd-8660-bce8e0759acb', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:59:30.057281+00', '2024-01-04 15:59:30.291683+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (2561, 181, NULL, NULL, 11, 2, 'e4a003a0-2184-4f2b-a4e2-4ed85a36f734', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:53:30.132639+00', '2024-01-04 15:53:30.486242+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (2562, 182, NULL, NULL, 11, 2, 'a3c10ea0-01c2-43f1-bfe7-3b25b7902dfb', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:55:30.075869+00', '2024-01-04 15:55:30.334252+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (2545, 183, NULL, NULL, 11, 2, '0e5d566a-00d9-4d02-bcaf-f01f7c582f99', 'wysXTgRikGN6nMB8AJ0JrQ==', NULL, '2024-01-04 15:58:30.071231+00', '2024-01-04 15:58:30.345809+00', 0, 0, NULL, NULL, NULL, NULL, NULL),

                (2750, 184, NULL, NULL, 2, 3, '8b7dff0f-a2e7-4210-8a5e-f216d8c874eb', 'wysXTgRikGN6nMB8AJ0JrQ==', 1, '2024-01-22 16:10:17.427415+00', '2024-01-22 16:10:17.681265+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (2759, 181, NULL, NULL, 2, 3, '71c3e02b-b7d2-4603-be74-e8c39faaf285', 'wysXTgRikGN6nMB8AJ0JrQ==', 1, '2024-01-22 16:10:19.834748+00', '2024-01-22 16:10:20.093688+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (2763, 182, NULL, NULL, 2, 3, '0dde5ec4-d16d-4940-a923-a73bacd969bb', 'wysXTgRikGN6nMB8AJ0JrQ==', 1, '2024-01-22 16:10:20.882012+00', '2024-01-22 16:10:21.144768+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (2766, 183, NULL, NULL, 2, 3, '2ba50586-e892-4ff9-a11a-67c38d23837a', 'wysXTgRikGN6nMB8AJ0JrQ==', 1, '2024-01-22 16:10:21.677894+00', '2024-01-22 16:10:21.933572+00', 0, 0, NULL, NULL, NULL, NULL, NULL),

                (2770, 185, NULL, NULL, 2, 1, '8b7dff0f-a2e7-4210-8a5e-f216d8c874eb', 'wysXTgRikGN6nMB8AJ0JrQ==', 1, '2024-01-22 16:10:17.427415+00', '2024-01-22 16:10:17.681265+00', 0, 0, NULL, NULL, NULL, NULL, NULL),
                (2771, 186, NULL, NULL, 2, 1, '71c3e02b-b7d2-4603-be74-e8c39faaf285', 'wysXTgRikGN6nMB8AJ0JrQ==', 1, '2024-01-22 16:10:19.834748+00', '2024-01-22 16:10:20.093688+00', 0, 0, NULL, NULL, NULL, NULL, NULL);

                INSERT INTO darts.media_request (mer_id, hea_id, requestor, request_status, request_type, req_proc_attempts, start_ts, end_ts, created_ts, last_modified_ts, created_by, last_modified_by, current_owner)
                VALUES
                (421, 101, -10, 'OPEN', 'DOWNLOAD', 0, '2024-01-04 11:00:02+00', '2024-01-04 11:00:19+00', '2024-01-22 15:41:04.393833+00', '2024-01-22 15:41:04.393866+00', -10, -10, -10);
                """);
    }

    @AfterAll
    void afterAll() {
        jdbcTemplate.update(
            """
                DELETE FROM darts.media_request WHERE hea_id=101;
                DELETE FROM darts.external_object_directory WHERE eod_id>=2541;
                DELETE FROM darts.hearing_media_ae WHERE hea_id=101;
                DELETE FROM darts.media WHERE ctr_id=-1;

                DELETE FROM darts.hearing WHERE hea_id=101;
                DELETE FROM darts.court_case WHERE cas_id=-1;
                DELETE FROM darts.courtroom WHERE ctr_id=-1;

                DELETE FROM darts.security_group_courthouse_ae WHERE grp_id=-4 AND cth_id=-1;
                DELETE FROM darts.security_group_user_account_ae WHERE usr_id=-10 AND grp_id=-4;
                DELETE FROM darts.user_account WHERE usr_id=-10;

                DELETE FROM darts.courthouse WHERE cth_id=-1;
                """);
    }

    @Test
    void givenAudioRequestBeingProcessedFromArchive_thenReturnResults() throws Exception {
        Integer mediaRequestId = 421;
        final List<AudioRequestBeingProcessedFromArchiveQueryResult> results = audioRequestBeingProcessedFromArchiveQuery.getResults(
            mediaRequestId);

        List expected = List.of(
            new AudioRequestBeingProcessedFromArchiveQueryResult(181, 2561, 2759),
            new AudioRequestBeingProcessedFromArchiveQueryResult(182, 2562, 2763),
            new AudioRequestBeingProcessedFromArchiveQueryResult(183, 2545, 2766),
            new AudioRequestBeingProcessedFromArchiveQueryResult(184, 2547, 2750)
        );
        assertEquals(expected.size(), results.size());
        assertEquals(expected, results);
    }

    @Test
    void givenAudioRequestBeingProcessedFromArchiveByMediaId_thenReturnResults() {
        // from a list of 4, only 2 meet the query criteria
        List<Integer> mediaIds = Arrays.asList(181, 182, 185, 186);
        final List<AudioRequestBeingProcessedFromArchiveQueryResult> results = audioRequestBeingProcessedFromArchiveQuery.getResultsByMediaIds(
            mediaIds);

        List<AudioRequestBeingProcessedFromArchiveQueryResult> expected = List.of(
            new AudioRequestBeingProcessedFromArchiveQueryResult(181, 2561, 2759),
            new AudioRequestBeingProcessedFromArchiveQueryResult(182, 2562, 2763)
        );
        assertEquals(expected.size(), results.size());
        assertEquals(expected, results);
    }
}
