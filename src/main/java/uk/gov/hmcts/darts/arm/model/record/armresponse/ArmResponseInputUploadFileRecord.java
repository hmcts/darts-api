package uk.gov.hmcts.darts.arm.model.record.armresponse;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class ArmResponseInputUploadFileRecord {

    @JsonProperty("operation")
    private String operation;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;

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