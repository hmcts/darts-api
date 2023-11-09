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
    public List<YourTranscriptsSummary> getRequesterTranscriptions(Integer userId) {
        return jdbcTemplate.query(
            """
                -- "requester_transcriptions"
                SELECT
                    tra.tra_id as transcription_id,
                    tra.cas_id as case_id,
                    cas.case_number,
                    cth.courthouse_name,
                    hea.hearing_date,
                    trt.description as transcription_type,
                    trs.status_type as status,
                    tru.description as urgency,
                    trw.workflow_ts as requested_ts
                FROM
                    transcription_workflow trw
                INNER JOIN
                    transcription tra
                ON
                    trw.tra_id = tra.tra_id
                INNER JOIN
                    court_case cas
                ON
                    tra.cas_id = cas.cas_id
                INNER JOIN
                    courthouse cth
                ON
                    cas.cth_id = cth.cth_id
                INNER JOIN
                    hearing hea
                ON
                    tra.hea_id = hea.hea_id
                INNER JOIN
                    transcription_type trt
                ON
                    tra.trt_id = trt.trt_id
                INNER JOIN
                    transcription_status trs
                ON
                    tra.trs_id = trs.trs_id
                INNER JOIN
                    transcription_urgency tru
                ON
                    tra.tru_id = tru.tru_id
                WHERE
                    trw.workflow_actor = :usr_id
                AND trw.trs_id = 1
                AND tra.trs_id <> 7

                UNION

                -- Migrated "requester_transcriptions"
                SELECT
                    tra.tra_id as transcription_id,
                    tra.cas_id as case_id,
                    cas.case_number,
                    cth.courthouse_name,
                    tra.hearing_date,
                    trt.description as transcription_type,
                    trs.status_type as status,
                    tru.description as urgency,
                    trw.workflow_ts as requested_ts
                FROM
                    transcription_workflow trw
                INNER JOIN
                    transcription tra
                ON
                    trw.tra_id = tra.tra_id
                INNER JOIN
                    court_case cas
                ON
                    tra.cas_id = cas.cas_id
                INNER JOIN
                    courthouse cth
                ON
                    cas.cth_id = cth.cth_id
                INNER JOIN
                    transcription_type trt
                ON
                    tra.trt_id = trt.trt_id
                INNER JOIN
                    transcription_status trs
                ON
                    tra.trs_id = trs.trs_id
                INNER JOIN
                    transcription_urgency tru
                ON
                    tra.tru_id = tru.tru_id
                WHERE
                    trw.workflow_actor = :usr_id
                AND trw.trs_id = 1
                AND tra.trs_id <> 7
                AND tra.hea_id IS NULL
                """,
            new MapSqlParameterSource("usr_id", userId),
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
                    tra.cas_id as case_id,
                    cas.case_number,
                    cth.courthouse_name,
                    hea.hearing_date,
                    trt.description as transcription_type,
                    trs.status_type as status,
                    tru.description as urgency,
                    trw.workflow_ts as requested_ts
                FROM
                    transcription tra
                INNER JOIN
                    court_case cas
                ON
                    tra.cas_id = cas.cas_id
                INNER JOIN
                    courthouse cth
                ON
                    cas.cth_id = cth.cth_id
                AND cth.cth_id IN
                    (   SELECT
                            DISTINCT(grc.cth_id)
                        FROM
                            user_account usr
                        INNER JOIN
                            security_group_user_account_ae gua
                        ON
                            usr.usr_id = gua.usr_id
                        INNER JOIN
                            security_group grp
                        ON
                            gua.grp_id = grp.grp_id
                        INNER JOIN
                            security_group_courthouse_ae grc
                        ON
                            grp.grp_id = grc.grp_id
                        WHERE
                            usr.usr_id = :usr_id
                        AND grp.rol_id = :rol_id)
                INNER JOIN
                    hearing hea
                ON
                    tra.hea_id = hea.hea_id
                INNER JOIN
                    transcription_type trt
                ON
                    tra.trt_id = trt.trt_id
                INNER JOIN
                    transcription_status trs
                ON
                    tra.trs_id = trs.trs_id
                INNER JOIN
                    transcription_urgency tru
                ON
                    tra.tru_id = tru.tru_id
                INNER JOIN
                    transcription_workflow trw
                ON
                    tra.tra_id = trw.tra_id
                AND trw.trs_id = 1
                WHERE
                    tra.trs_id = :trs_id
                AND trw.workflow_actor <> :usr_id

                UNION

                -- Migrated approver_transcriptions
                SELECT
                    tra.tra_id as transcription_id,
                    tra.cas_id as case_id,
                    cas.case_number,
                    cth.courthouse_name,
                    hearing_date,
                    trt.description as transcription_type,
                    trs.status_type as status,
                    tru.description as urgency,
                    trw.workflow_ts as requested_ts
                FROM
                    transcription tra
                INNER JOIN
                    court_case cas
                ON
                    tra.cas_id = cas.cas_id
                INNER JOIN
                    courthouse cth
                ON
                    cas.cth_id = cth.cth_id
                AND cth.cth_id IN
                    (   SELECT
                            DISTINCT(grc.cth_id)
                        FROM
                            user_account usr
                        INNER JOIN
                            security_group_user_account_ae gua
                        ON
                            usr.usr_id = gua.usr_id
                        INNER JOIN
                            security_group grp
                        ON
                            gua.grp_id = grp.grp_id
                        INNER JOIN
                            security_group_courthouse_ae grc
                        ON
                            grp.grp_id = grc.grp_id
                        WHERE
                            usr.usr_id = :usr_id
                        AND grp.rol_id = :rol_id)
                INNER JOIN
                    transcription_type trt
                ON
                    tra.trt_id = trt.trt_id
                INNER JOIN
                    transcription_status trs
                ON
                    tra.trs_id = trs.trs_id
                INNER JOIN
                    transcription_urgency tru
                ON
                    tra.tru_id = tru.tru_id
                INNER JOIN
                    transcription_workflow trw
                ON
                    tra.tra_id = trw.tra_id
                AND trw.trs_id = 1
                WHERE
                    tra.trs_id = :trs_id
                AND trw.workflow_actor <> :usr_id
                AND tra.hea_id IS NULL
                """,
            new MapSqlParameterSource("usr_id", userId)
                .addValue("rol_id", APPROVER.getId())
                .addValue("trs_id", AWAITING_AUTHORISATION.getId()),
            yourTranscriptsSummaryRowMapper
        );
    }

}
