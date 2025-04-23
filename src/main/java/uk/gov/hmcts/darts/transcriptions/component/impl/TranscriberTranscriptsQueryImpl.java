package uk.gov.hmcts.darts.transcriptions.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.transcriptions.component.TranscriberTranscriptsQuery;
import uk.gov.hmcts.darts.transcriptions.model.TranscriberViewSummary;
import uk.gov.hmcts.darts.transcriptions.util.TranscriptionUtil;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;

import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@Component
@RequiredArgsConstructor
public class TranscriberTranscriptsQueryImpl implements TranscriberTranscriptsQuery {

    private static final String USR_ID = "usr_id";
    private static final String ROL_ID = "rol_id";
    private static final String DATE_LIMIT = "date_limit";
    private static final String MAX_RESULT_SIZE = "max_result_size";
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TranscriberViewSummaryRowMapper transcriberViewSummaryRowMapper;

    @Value("${darts.transcription.search.date-limit}")
    private final Duration dateLimit;

    @Value("${darts.transcription.search.max-result-size}")
    private final BigInteger maxResultSize;

    @Override
    public List<TranscriberViewSummary> getTranscriptRequests(Integer userId) {

        return jdbcTemplate.query(
            """
                -- Transcript Requests (transcriber-view?assigned=false)
                SELECT
                    tra.tra_id as transcription_id,
                    cas.cas_id as case_id,
                    cas.case_number,
                    cth.display_name courthouse_name,
                    hea.hearing_date,
                    trt.description                  as transcription_type,
                    trs.display_name                 as status,
                    tru.description                  as transcription_urgency_description,
                    tru.tru_id                       as transcription_urgency_id,
                    tru.priority_order               as transcription_urgency_priority_order,
                    tra.is_manual_transcription      as is_manual,
                    (SELECT MIN(workflow_ts) FROM darts.transcription_workflow w WHERE w.tra_id = tra.tra_id AND w.trs_id = 1) as requested_ts,
                    (SELECT MAX(workflow_ts) FROM darts.transcription_workflow w WHERE w.tra_id = tra.tra_id AND w.trs_id = tra.trs_id) as state_change_ts,
                    (SELECT MIN(workflow_ts) FROM darts.transcription_workflow w WHERE w.tra_id = tra.tra_id AND w.trs_id = 3) as approved_ts
                FROM darts.transcription tra
                JOIN darts.case_transcription_ae case_transcription ON tra.tra_id = case_transcription.tra_id
                JOIN darts.court_case cas ON case_transcription.cas_id = cas.cas_id
                JOIN darts.courthouse cth ON cas.cth_id = cth.cth_id
                AND cth.cth_id IN (
                    SELECT DISTINCT(grc.cth_id)
                    FROM darts.user_account usr
                    JOIN darts.security_group_user_account_ae gua ON usr.usr_id = gua.usr_id
                    JOIN darts.security_group grp ON gua.grp_id = grp.grp_id
                    JOIN darts.security_group_courthouse_ae grc ON grp.grp_id = grc.grp_id
                    WHERE usr.usr_id = :usr_id
                    AND grp.rol_id = :rol_id
                    AND usr.is_active = true
                )
                JOIN darts.hearing_transcription_ae hearing_transcription ON tra.tra_id = hearing_transcription.tra_id
                JOIN darts.hearing hea ON hearing_transcription.hea_id = hea.hea_id
                JOIN darts.transcription_type trt ON tra.trt_id = trt.trt_id
                JOIN darts.transcription_status trs ON tra.trs_id = trs.trs_id
                LEFT JOIN darts.transcription_urgency tru ON tra.tru_id = tru.tru_id
                WHERE (SELECT MAX(workflow_ts) FROM darts.transcription_workflow w WHERE w.tra_id = tra.tra_id AND w.trs_id = tra.trs_id) >= :date_limit
                AND tra.is_current = true
                AND tra.trs_id = 3
                ORDER BY cas.case_number desc
                LIMIT :max_result_size
                """,
            new MapSqlParameterSource()
                .addValue(USR_ID, userId)
                .addValue(ROL_ID, TRANSCRIBER.getId())
                .addValue(DATE_LIMIT, TranscriptionUtil.getDateToLimitResults(dateLimit))
                .addValue(MAX_RESULT_SIZE, maxResultSize),
            transcriberViewSummaryRowMapper
        );
    }

    @Override
    public List<TranscriberViewSummary> getTranscriberTranscriptions(Integer userId) {

        return jdbcTemplate.query(
            """
                -- Your work > To do (transcriber-view?assigned=true)
                SELECT
                    tra.tra_id as transcription_id,
                    cas.cas_id as case_id,
                    cas.case_number,
                    cth.display_name courthouse_name,
                    hea.hearing_date,
                    trt.description                  as transcription_type,
                    trs.display_name                 as status,
                    tru.description                  as transcription_urgency_description,
                    tru.tru_id                       as transcription_urgency_id,
                    tru.priority_order               as transcription_urgency_priority_order,
                    requested_trw.workflow_ts        as requested_ts,
                    with_transcriber_trw.workflow_ts as state_change_ts,
                    tra.is_manual_transcription      as is_manual,
                    (SELECT MIN(workflow_ts) FROM darts.transcription_workflow w WHERE w.tra_id = tra.tra_id AND w.trs_id = 3) as approved_ts
                FROM darts.transcription tra
                JOIN darts.case_transcription_ae case_transcription ON tra.tra_id = case_transcription.tra_id
                JOIN darts.court_case cas ON case_transcription.cas_id = cas.cas_id
                JOIN darts.courthouse cth ON cas.cth_id = cth.cth_id
                AND cth.cth_id IN (
                    SELECT DISTINCT(grc.cth_id)
                    FROM darts.user_account usr
                    JOIN darts.security_group_user_account_ae gua ON usr.usr_id = gua.usr_id
                    JOIN darts.security_group grp ON gua.grp_id = grp.grp_id
                    JOIN darts.security_group_courthouse_ae grc ON grp.grp_id = grc.grp_id
                    WHERE usr.usr_id = :usr_id
                    AND grp.rol_id = :rol_id
                    AND usr.is_active = true
                )
                JOIN darts.hearing_transcription_ae hearing_transcription ON tra.tra_id = hearing_transcription.tra_id
                JOIN darts.hearing hea ON hearing_transcription.hea_id = hea.hea_id
                JOIN darts.transcription_type trt ON tra.trt_id = trt.trt_id
                JOIN darts.transcription_status trs ON tra.trs_id = trs.trs_id
                LEFT JOIN darts.transcription_urgency tru ON tra.tru_id = tru.tru_id
                JOIN darts.transcription_workflow requested_trw ON tra.tra_id = requested_trw.tra_id
                    AND requested_trw.trs_id = 1
                -- Only the latest "WITH_TRANSCRIBER" transcription_workflow for a given transcription
                JOIN (
                    SELECT trw.tra_id, MAX(trw.workflow_ts) as workflow_ts
                    FROM darts.transcription_workflow trw
                    WHERE trw.trs_id = 5
                    AND trw.workflow_actor = :usr_id
                    GROUP BY tra_id
                ) with_transcriber_trw ON with_transcriber_trw.tra_id = tra.tra_id
                WHERE tra.trs_id = 5
                AND requested_trw.workflow_ts >= :date_limit
                -- exclude ones with hidden docs - just in case there are any
                AND (
                    EXISTS (
                        SELECT 1
                        FROM darts.transcription_document trd
                        WHERE trd.tra_id = tra.tra_id
                        AND trd.is_hidden = false
                    )
                    OR
                    NOT EXISTS (
                        SELECT 1
                        FROM darts.transcription_document trd
                        WHERE trd.tra_id = tra.tra_id
                    )
                )
                AND tra.is_current = true
                
                UNION
                
                -- Your work > Completed today (transcriber-view?assigned=true)
                SELECT
                    tra.tra_id as transcription_id,
                    cas.cas_id as case_id,
                    cas.case_number,
                    cth.display_name courthouse_name,
                    hea.hearing_date,
                    trt.description             as transcription_type,
                    trs.display_name            as status,
                    tru.description             as transcription_urgency_description,
                    tru.tru_id                  as transcription_urgency_id,
                    tru.priority_order          as transcription_urgency_priority_order,
                    (SELECT MIN(workflow_ts) FROM darts.transcription_workflow w WHERE w.tra_id = tra.tra_id AND w.trs_id = 1) as requested_ts,
                    complete_trw.workflow_ts    as state_change_ts,
                    tra.is_manual_transcription as is_manual,
                    (SELECT MIN(workflow_ts) FROM darts.transcription_workflow w WHERE w.tra_id = tra.tra_id AND w.trs_id = 3) as approved_ts
                FROM darts.transcription tra
                JOIN darts.case_transcription_ae case_transcription ON tra.tra_id = case_transcription.tra_id
                JOIN darts.court_case cas ON case_transcription.cas_id = cas.cas_id
                JOIN darts.courthouse cth ON cas.cth_id = cth.cth_id
                AND cth.cth_id IN (
                    SELECT DISTINCT(grc.cth_id)
                    FROM darts.user_account usr
                    JOIN darts.security_group_user_account_ae gua ON usr.usr_id = gua.usr_id
                    JOIN darts.security_group grp ON gua.grp_id = grp.grp_id
                    JOIN darts.security_group_courthouse_ae grc ON grp.grp_id = grc.grp_id
                    WHERE usr.usr_id = :usr_id
                    AND grp.rol_id = :rol_id
                    AND usr.is_active = true
                )
                JOIN darts.hearing_transcription_ae hearing_transcription ON tra.tra_id = hearing_transcription.tra_id
                JOIN darts.hearing hea ON hearing_transcription.hea_id = hea.hea_id
                JOIN darts.transcription_type trt ON tra.trt_id = trt.trt_id
                JOIN darts.transcription_status trs ON tra.trs_id = trs.trs_id
                LEFT JOIN darts.transcription_urgency tru ON tra.tru_id = tru.tru_id
                JOIN darts.transcription_workflow requested_trw ON tra.tra_id = requested_trw.tra_id
                    AND requested_trw.trs_id = 1
                JOIN darts.transcription_workflow complete_trw ON tra.tra_id = complete_trw.tra_id
                    AND complete_trw.trs_id = 6
                    AND complete_trw.workflow_actor = :usr_id
                WHERE tra.trs_id = 6
                AND complete_trw.workflow_ts >= CURRENT_DATE
                AND (
                    EXISTS (
                        SELECT 1
                        FROM darts.transcription_document trd
                        WHERE trd.tra_id = tra.tra_id
                        AND trd.is_hidden = false
                    )
                    OR
                    NOT EXISTS (
                        SELECT 1
                        FROM darts.transcription_document trd
                        WHERE trd.tra_id = tra.tra_id
                    )
                )
                AND tra.is_current = true
                ORDER BY case_number desc
                LIMIT :max_result_size
                """,
            new MapSqlParameterSource()
                .addValue(USR_ID, userId)
                .addValue(ROL_ID, TRANSCRIBER.getId())
                .addValue(DATE_LIMIT, TranscriptionUtil.getDateToLimitResults(dateLimit))
                .addValue(MAX_RESULT_SIZE, maxResultSize),
            transcriberViewSummaryRowMapper
        );
    }

    @Override
    public List<Integer> getAuthorisedCourthouses(Integer userId, Integer roleId) {
        return jdbcTemplate.queryForList(
            """
                select
                    distinct courthouse.cth_id
                from
                    darts.user_account user_account
                join
                    (darts.security_group_user_account_ae security_group_user_account_ae
                join
                    darts.security_group security_group
                        on security_group.grp_id=security_group_user_account_ae.grp_id)
                            on user_account.usr_id=security_group_user_account_ae.usr_id
                    join
                        (darts.security_group_courthouse_ae security_group_courthouse_ae
                    join
                        darts.courthouse courthouse
                            on courthouse.cth_id=security_group_courthouse_ae.cth_id)
                                on security_group.grp_id=security_group_courthouse_ae.grp_id
                        where
                            user_account.usr_id=:usr_id
                            and security_group.rol_id in (:rol_id)
                            and user_account.is_Active=true
                """,
            new MapSqlParameterSource(USR_ID, userId)
                .addValue(ROL_ID, roleId),
            Integer.class
        );
    }

    @Override
    public Integer getTranscriptionsCountForCourthouses(List<Integer> courthouseIds, Integer transcriptionStatusId, int userId) {
        // only the latest "APPROVED" OR "WITH_TRANSCRIBER" transcription_workflow for a given transcription
        StringBuilder workflowSubQuery = new StringBuilder(174)
            .append(
                """
                        SELECT trw.tra_id, MAX(trw.workflow_ts) as workflow_ts
                        FROM darts.transcription_workflow trw
                        WHERE trw.trs_id = :trs_id
                    """);
        if (transcriptionStatusId.equals(WITH_TRANSCRIBER.getId())) {
            workflowSubQuery.append(" AND trw.workflow_actor = ").append(userId);
        }
        workflowSubQuery.append(" GROUP BY tra_id");

        StringBuilder sql = new StringBuilder(433)
            .append(
                """
                    SELECT count(*)
                    FROM darts.transcription transcription
                    JOIN darts.case_transcription_ae case_transcription ON transcription.tra_id = case_transcription.tra_id
                    JOIN darts.court_case court_case ON case_transcription.cas_id = court_case.cas_id
                    JOIN darts.courthouse courthouse ON courthouse.cth_id=court_case.cth_id
                    JOIN (""")
            .append(workflowSubQuery)
            .append(
                """
                    ) trw ON trw.tra_id = transcription.tra_id
                    WHERE court_case.cth_id IN (:cth_ids)
                    AND transcription.trs_id=:trs_id
                    """);

        return jdbcTemplate.queryForObject(
            sql.toString(),
            new MapSqlParameterSource("cth_ids", courthouseIds)
                .addValue("trs_id", transcriptionStatusId),
            Integer.class
        );
    }

}