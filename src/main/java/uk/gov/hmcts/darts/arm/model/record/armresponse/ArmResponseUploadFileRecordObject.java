package uk.gov.hmcts.darts.arm.model.record.armresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;

import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.UPLOAD_NEW_FILE;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@SuppressWarnings({"PMD.FinalFieldCouldBeStatic"})
public class ArmResponseUploadFileRecordObject {
    private final String operation = UPLOAD_NEW_FILE;
    private String transactionId;
    private String relationId;
    private String a360RecordId;
    private String processTime;
    private Integer status;
    private UploadNewFileRecord input;
    private String exceptionDescription;
    private String errorStatus;
    private String a360FileId;
    private Integer fileSize;
    private String md5;
    private String sha256;
}

