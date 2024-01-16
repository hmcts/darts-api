package uk.gov.hmcts.darts.arm.model.record.armresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.UPLOAD_NEW_FILE;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class ArmResponseUploadFileRecord {
    @JsonProperty("operation")
    private final String operation = UPLOAD_NEW_FILE;
    @JsonProperty("transaction_id")
    private String transactionId;
    @JsonProperty("relation_id")
    private String relationId;
    @JsonProperty("a360_record_id")
    private String a360RecordId;
    @JsonProperty("process_time")
    private String processTime;
    @JsonProperty("status")
    private Integer status;
    @JsonProperty("input")
    private String input;
    @JsonProperty("exception_description")
    private String exceptionDescription;
    @JsonProperty("error_status")
    private String errorStatus;
    @JsonProperty("a360_file_id")
    private String a360FileId;
    @JsonProperty("file_size")
    private Integer fileSize;
    @JsonProperty("s_md5")
    private String md5;
    @JsonProperty("s_sha256")
    private String sha256;
}

