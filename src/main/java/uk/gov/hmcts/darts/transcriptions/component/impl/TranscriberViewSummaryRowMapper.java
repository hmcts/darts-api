package uk.gov.hmcts.darts.transcriptions.component.impl;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.transcriptions.model.TranscriberViewSummary;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyDetails;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
public class TranscriberViewSummaryRowMapper implements RowMapper<TranscriberViewSummary> {

    @Override
    public TranscriberViewSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
        var summary = new TranscriberViewSummary(
            rs.getInt("transcription_id"),
            rs.getInt("case_id"),
            rs.getString("case_number"),
            rs.getString("courthouse_name"),
            rs.getObject("hearing_date", LocalDate.class),
            rs.getString("transcription_type"),
            rs.getString("status"),
            rs.getObject("requested_ts", OffsetDateTime.class),
            rs.getObject("state_change_ts", OffsetDateTime.class),
            rs.getBoolean("is_manual")
        );

        summary.setApprovedTs(rs.getObject("approved_ts", OffsetDateTime.class));
        if (rs.getInt("transcription_urgency_id") != 0) {
            TranscriptionUrgencyDetails urgencyDetails = new TranscriptionUrgencyDetails();
            urgencyDetails.setTranscriptionUrgencyId(rs.getInt("transcription_urgency_id"));
            urgencyDetails.setPriorityOrder(rs.getInt("transcription_urgency_priority_order"));
            urgencyDetails.setDescription(rs.getString("transcription_urgency_description"));
            summary.setTranscriptionUrgency(urgencyDetails);
        }
        return summary;
    }

}