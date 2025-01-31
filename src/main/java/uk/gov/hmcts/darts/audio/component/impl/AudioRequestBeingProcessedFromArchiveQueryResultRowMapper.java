package uk.gov.hmcts.darts.audio.component.impl;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.model.AudioRequestBeingProcessedFromArchiveQueryResult;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class AudioRequestBeingProcessedFromArchiveQueryResultRowMapper implements RowMapper<AudioRequestBeingProcessedFromArchiveQueryResult> {

    @Override
    public AudioRequestBeingProcessedFromArchiveQueryResult mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AudioRequestBeingProcessedFromArchiveQueryResult(rs.getInt("med_id"));
    }

}
