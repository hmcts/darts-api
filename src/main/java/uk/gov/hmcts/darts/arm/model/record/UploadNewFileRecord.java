package uk.gov.hmcts.darts.arm.model.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.darts.arm.model.ArchiveRecordOperation;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;

import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.UPLOAD_NEW_FILE;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class UploadNewFileRecord implements ArchiveRecordOperation {

    @JsonProperty("operation")
    private final String operation = UPLOAD_NEW_FILE;
    @JsonProperty("relation_id")
    private String relationId;
    @JsonProperty("file_metadata")
    private UploadNewFileRecordMetadata fileMetadata;

}
