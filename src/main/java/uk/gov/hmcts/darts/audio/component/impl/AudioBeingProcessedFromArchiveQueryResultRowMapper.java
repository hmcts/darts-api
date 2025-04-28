package uk.gov.hmcts.darts.audio.component.impl;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.model.AudioBeingProcessedFromArchiveQueryResult;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class AudioBeingProcessedFromArchiveQueryResultRowMapper implements RowMapper<AudioBeingProcessedFromArchiveQueryResult> {

    @Override
    public AudioBeingProcessedFromArchiveQueryResult mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AudioBeingProcessedFromArchiveQueryResult(
            rs.getLong("med_id"),
            rs.getInt("arm_eod_id")
        );
    }

}
