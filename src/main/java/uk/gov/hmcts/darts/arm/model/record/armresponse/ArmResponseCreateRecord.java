package uk.gov.hmcts.darts.arm.model.record.armresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.CREATE_RECORD;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@SuppressWarnings({"PMD.FinalFieldCouldBeStatic"})
public class ArmResponseCreateRecord {
    @JsonProperty("operation")
    private final String operation = CREATE_RECORD;
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

}
