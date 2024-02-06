package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.AudioRequestBeingProcessedFromArchiveQuery;
import uk.gov.hmcts.darts.audio.model.AudioRequestBeingProcessedFromArchiveQueryResult;

import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.DELETED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Component
@RequiredArgsConstructor
public class AudioRequestBeingProcessedFromArchiveQueryImpl implements AudioRequestBeingProcessedFromArchiveQuery {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AudioRequestBeingProcessedFromArchiveQueryResultRowMapper rowMapper;

    @Override
    public List<AudioRequestBeingProcessedFromArchiveQueryResult> getResults(Integer mediaRequestId) {
        return jdbcTemplate.query(
              """
                    SELECT
                        med.med_id,
                        eod_unstructured.eod_id AS unstructured_eod_id,
                        eod_arm.eod_id          AS arm_eod_id
                    FROM
                        darts.media_request mer
                    JOIN
                        darts.hearing hea
                    ON
                        mer.hea_id = hea.hea_id
                    JOIN
                        darts.hearing_media_ae hem
                    ON
                        hea.hea_id = hem.hea_id
                    JOIN
                        darts.media med
                    ON
                        med.med_id = hem.med_id
                    AND
                        (mer.start_ts >= med.start_ts
                        AND med.end_ts <= mer.end_ts)
                    JOIN
                        darts.external_object_directory eod_unstructured
                    ON
                        med.med_id = eod_unstructured.med_id
                    AND eod_unstructured.elt_id = :unstructured_elt_id
                    AND eod_unstructured.ors_id = :unstructured_ors_id
                    JOIN
                        darts.external_object_directory eod_arm
                    ON
                        med.med_id = eod_arm.med_id
                    AND eod_arm.elt_id = :arm_elt_id
                    AND eod_arm.ors_id = :arm_ors_id
                    WHERE
                        mer.mer_id = :mer_id
                    ORDER BY
                        med.med_id ASC
                    """,
              new MapSqlParameterSource()
                    .addValue("mer_id", mediaRequestId)
                    .addValue("unstructured_elt_id", UNSTRUCTURED.getId())
                    .addValue("unstructured_ors_id", DELETED.getId())
                    .addValue("arm_elt_id", ARM.getId())
                    .addValue("arm_ors_id", STORED.getId()),
              rowMapper
        );
    }

}
