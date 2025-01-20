package uk.gov.hmcts.darts.arm.model.record.armresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = false)
public class ArmResponseInputUploadFileRecord {

    @JsonProperty("operation")
    private String operation;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("exception_description")
    private String exceptionDescription;

    @JsonProperty("error_status")
    private String errorStatus;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("submission_folder")
    private String submissionFolder;

    @JsonProperty("file_hash")
    private String fileHash;
}