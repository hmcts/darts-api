package uk.gov.hmcts.darts.archiverecordsmanagement.model.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.ArchiveMetadata;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.ArchiveRecord;

import static uk.gov.hmcts.darts.archiverecordsmanagement.util.ARMConstants.UPLOAD_NEW_FILE;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UploadNewFileRecord implements ArchiveRecord {

    @JsonProperty("relation_id")
    private String relationId;
    @JsonProperty("file_metadata")
    private UploadNewFileRecordMetadata archiveMetadata;

    @Override
    public String getOperation() {
        return UPLOAD_NEW_FILE;
    }

    @Override
    @JsonProperty("file_metadata")
    public ArchiveMetadata getMetadata() {
        return archiveMetadata;
    }

}
