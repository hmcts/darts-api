package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.AudioBeingProcessedFromArchiveQuery;
import uk.gov.hmcts.darts.audio.model.AudioBeingProcessedFromArchiveQueryResult;

import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.DETS;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Component
@RequiredArgsConstructor
public class AudioBeingProcessedFromArchiveQueryImpl implements AudioBeingProcessedFromArchiveQuery {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AudioBeingProcessedFromArchiveQueryResultRowMapper rowMapper;

    @Override
    public List<AudioBeingProcessedFromArchiveQueryResult> getResults(Integer hearingId) {
        return jdbcTemplate.query(
            """
                SELECT
                    hem.med_id,
                    eod_arm.eod_id as arm_eod_id
                FROM
                    darts.hearing_media_ae hem
                JOIN
                    darts.external_object_directory eod_arm
                ON
                    hem.med_id = eod_arm.med_id
                WHERE
                    eod_arm.ors_id = :stored_ors_id
                AND
                    eod_arm.elt_id = :arm_elt_id
                AND
                    hem.hea_id = :hea_id
                AND NOT EXISTS
                (
                  SELECT eod_other.eod_id
                  FROM darts.external_object_directory eod_other
                  WHERE eod_arm.med_id = eod_other.med_id
                  AND eod_other.elt_id IN (:other_elt_id)
                )
                ORDER BY
                    hem.med_id ASC
                """,
            new MapSqlParameterSource()
                .addValue("hea_id", hearingId)
                .addValue("arm_elt_id", ARM.getId())
                .addValue("other_elt_id", List.of(UNSTRUCTURED.getId(), DETS.getId()))
                .addValue("stored_ors_id", STORED.getId()),
            rowMapper
        );
    }
}
