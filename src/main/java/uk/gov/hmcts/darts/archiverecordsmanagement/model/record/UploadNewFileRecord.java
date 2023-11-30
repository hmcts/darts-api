package uk.gov.hmcts.darts.archiverecordsmanagement.model.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.ArchiveRecord;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.metadata.UploadNewFileRecordMetadata;

import static uk.gov.hmcts.darts.archiverecordsmanagement.util.ArchiveConstants.ArchiveRecordOperationValues.UPLOAD_NEW_FILE;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class UploadNewFileRecord implements ArchiveRecord {

    @JsonProperty("operation")
    private final String operation = UPLOAD_NEW_FILE;
    @JsonProperty("relation_id")
    private String relationId;
    @JsonProperty("file_metadata")
    private UploadNewFileRecordMetadata fileMetadata;

}
