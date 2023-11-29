package uk.gov.hmcts.darts.archiverecordsmanagement.model.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.ArchiveMetadata;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UploadNewFileRecordMetadata implements ArchiveMetadata {
    @JsonProperty("publisher")
    private String publisher;
    @JsonProperty("dz_file_name")
    private String dzFilename;
    @JsonProperty("file_tag")
    private String fileTag;

}
