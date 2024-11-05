package uk.gov.hmcts.darts.arm.client.model.rpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class MasterIndexFieldByRecordClassSchemaRequest {

    private String recordClassCode;
    private Boolean isForSearch;
    private Integer fieldType;
    private Boolean usePaging;

}
