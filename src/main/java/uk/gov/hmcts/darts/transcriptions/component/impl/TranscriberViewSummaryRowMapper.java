package uk.gov.hmcts.darts.transcriptions.component.impl;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.transcriptions.model.TranscriberViewSummary;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
public class TranscriberViewSummaryRowMapper implements RowMapper<TranscriberViewSummary> {

    @Override
    public TranscriberViewSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
        var summary = new TranscriberViewSummary(
            Integer.valueOf(rs.getInt("transcription_id")),
            Integer.valueOf(rs.getInt("case_id")),
            rs.getString("case_number"),
            rs.getString("courthouse_name"),
            rs.getObject("hearing_date", LocalDate.class),
            rs.getString("transcription_type"),
            rs.getString("status"),
            rs.getObject("requested_ts", OffsetDateTime.class),
            rs.getObject("requested_ts", OffsetDateTime.class),
            rs.getBoolean("is_manual")
        );
        summary.setUrgency(rs.getString("urgency"));
        return summary;
    }

}
