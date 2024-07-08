package uk.gov.hmcts.darts.transcriptions.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.transcriptions.component.YourTranscriptsQuery;
import uk.gov.hmcts.darts.transcriptions.model.YourTranscriptsSummary;

import java.util.List;

import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;

@Component
@RequiredArgsConstructor
public class YourTranscriptsQueryImpl implements YourTranscriptsQuery {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final YourTranscriptsSummaryRowMapper yourTranscriptsSummaryRowMapper;

    @Override
    public List<YourTranscriptsSummary> getRequesterTranscriptions(Integer userId, Boolean includeHiddenFromRequester) {
        return jdbcTemplate.query(
            """
                -- "requester_transcriptions"
                SELECT
                    tra.tra_id as transcription_id,
                    cas.cas_id as case_id,
                    cas.case_number,
                    cth.courthouse_name,
                    hea.hearing_date,
                    trt.description as transcription_type,
                    trs.display_name as status,
                    tru.description as transcription_urgency_description,
                    tru.tru_id  as transcription_urgency_id,
                    tru.priority_order as transcription_urgency_priority_order,
                    trw.workflow_ts as requested_ts
                FROM darts.transcription_workflow trw
                JOIN darts.transcription tra ON trw.tra_id = tra.tra_id
                JOIN darts.case_transcription_ae case_transcription ON tra.tra_id = case_transcription.tra_id
                JOIN darts.court_case cas ON case_transcription.cas_id = cas.cas_id
                JOIN darts.courthouse cth ON cas.cth_id = cth.cth_id
                JOIN darts.hearing_transcription_ae hearing_transcription ON tra.tra_id = hearing_transcription.tra_id
                JOIN darts.hearing hea ON hearing_transcription.hea_id = hea.hea_id
                JOIN darts.transcription_type trt ON tra.trt_id = trt.trt_id
                JOIN darts.transcription_status trs ON tra.trs_id = trs.trs_id
                LEFT JOIN darts.transcription_urgency tru ON tra.tru_id = tru.tru_id
                WHERE trw.workflow_actor = :usr_id
                AND trw.trs_id = 1
                AND tra.trs_id <> 7
                AND (:include_hidden_from_requester OR tra.hide_request_from_requestor = false)
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

                UNION

                -- Migrated "requester_transcriptions"
                SELECT
                    tra.tra_id as transcription_id,
                    cas.cas_id as case_id,
                    cas.case_number,
                    cth.courthouse_name,
                    tra.hearing_date,
                    trt.description as transcription_type,
                    trs.display_name as status,
                    tru.description as transcription_urgency_description,
                    tru.tru_id  as transcription_urgency_id,
                    tru.priority_order as transcription_urgency_priority_order,
                    trw.workflow_ts as requested_ts
                FROM darts.transcription_workflow trw
                JOIN darts.transcription tra ON trw.tra_id = tra.tra_id
                JOIN darts.case_transcription_ae case_transcription ON tra.tra_id = case_transcription.tra_id
                JOIN darts.court_case cas ON case_transcription.cas_id = cas.cas_id
                JOIN darts.courthouse cth ON cas.cth_id = cth.cth_id
                JOIN darts.transcription_type trt ON tra.trt_id = trt.trt_id
                JOIN darts.transcription_status trs ON tra.trs_id = trs.trs_id
                LEFT JOIN darts.transcription_urgency tru ON tra.tru_id = tru.tru_id
                WHERE trw.workflow_actor = :usr_id
                AND trw.trs_id = 1
                AND tra.trs_id <> 7
                AND NOT EXISTS (
                    SELECT 1
                    FROM darts.hearing_transcription_ae hear_tran
                    WHERE tra.tra_id = hear_tran.tra_id
                )
                AND (:include_hidden_from_requester OR tra.hide_request_from_requestor = false)
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
                ORDER BY transcription_id DESC
                """,
            new MapSqlParameterSource()
                .addValue("usr_id", userId)
                .addValue("include_hidden_from_requester", includeHiddenFromRequester),
            yourTranscriptsSummaryRowMapper
        );
    }

    @Override
    public List<YourTranscriptsSummary> getApproverTranscriptions(Integer userId) {
        return jdbcTemplate.query(
            """
                -- approver_transcriptions
                SELECT
                    tra.tra_id as transcription_id,
                    cas.cas_id as case_id,
                    cas.case_number,
                    cth.courthouse_name,
                    hea.hearing_date,
                    trt.description as transcription_type,
                    trs.display_name as status,
                    tru.description as transcription_urgency_description,
                    tru.tru_id  as transcription_urgency_id,
                    tru.priority_order as transcription_urgency_priority_order,
                    trw.workflow_ts as requested_ts
                FROM darts.transcription tra
                JOIN darts.case_transcription_ae case_transcription ON tra.tra_id = case_transcription.tra_id
                JOIN darts.court_case cas ON case_transcription.cas_id = cas.cas_id
                JOIN darts.courthouse cth ON cas.cth_id = cth.cth_id AND cth.cth_id IN (
                    SELECT DISTINCT(grc.cth_id)
                    FROM darts.user_account usr
                    JOIN darts.security_group_user_account_ae gua ON usr.usr_id = gua.usr_id
                    JOIN darts.security_group grp ON gua.grp_id = grp.grp_id
                    JOIN darts.security_group_courthouse_ae grc ON grp.grp_id = grc.grp_id
                    WHERE  usr.usr_id = :usr_id
                    AND grp.rol_id = :rol_id
                    AND usr.is_active = true
                )
                JOIN darts.hearing_transcription_ae hearing_transcription ON tra.tra_id = hearing_transcription.tra_id
                JOIN darts.hearing hea ON hearing_transcription.hea_id = hea.hea_id
                JOIN darts.transcription_type trt ON tra.trt_id = trt.trt_id
                JOIN darts.transcription_status trs ON tra.trs_id = trs.trs_id
                LEFT JOIN darts.transcription_urgency tru ON tra.tru_id = tru.tru_id
                JOIN darts.transcription_workflow trw ON tra.tra_id = trw.tra_id
                AND trw.trs_id = 1
                WHERE tra.trs_id = :trs_id
                AND trw.workflow_actor <> :usr_id
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

                UNION

                -- Migrated approver_transcriptions
                SELECT
                    tra.tra_id as transcription_id,
                    cas.cas_id as case_id,
                    cas.case_number,
                    cth.courthouse_name,
                    hearing_date,
                    trt.description as transcription_type,
                    trs.display_name as status,
                    tru.description as transcription_urgency_description,
                    tru.tru_id  as transcription_urgency_id,
                    tru.priority_order as transcription_urgency_priority_order,
                    trw.workflow_ts as requested_ts
                FROM darts.transcription tra
                JOIN darts.case_transcription_ae case_transcription ON tra.tra_id = case_transcription.tra_id
                JOIN darts.court_case cas ON case_transcription.cas_id = cas.cas_id
                JOIN darts.courthouse cth ON cas.cth_id = cth.cth_id AND cth.cth_id IN (
                    SELECT DISTINCT(grc.cth_id)
                    FROM darts.user_account usr
                    JOIN darts.security_group_user_account_ae gua ON usr.usr_id = gua.usr_id
                    JOIN darts.security_group grp ON gua.grp_id = grp.grp_id
                    JOIN darts.security_group_courthouse_ae grc ON grp.grp_id = grc.grp_id
                    WHERE usr.usr_id = :usr_id
                    AND usr.is_active = true
                    AND grp.rol_id = :rol_id
                )
                JOIN darts.transcription_type trt ON tra.trt_id = trt.trt_id
                JOIN darts.transcription_status trs ON tra.trs_id = trs.trs_id
                LEFT JOIN darts.transcription_urgency tru ON tra.tru_id = tru.tru_id
                JOIN darts.transcription_workflow trw ON tra.tra_id = trw.tra_id
                AND trw.trs_id = 1
                WHERE tra.trs_id = :trs_id
                AND trw.workflow_actor <> :usr_id
                AND NOT EXISTS (
                    SELECT  1
                    FROM darts.hearing_transcription_ae hear_tran
                    WHERE tra.tra_id = hear_tran.tra_id
                )
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
                ORDER BY transcription_id DESC
                """,
            new MapSqlParameterSource("usr_id", userId)
                .addValue("rol_id", APPROVER.getId())
                .addValue("trs_id", AWAITING_AUTHORISATION.getId()),
            yourTranscriptsSummaryRowMapper
        );
    }

}