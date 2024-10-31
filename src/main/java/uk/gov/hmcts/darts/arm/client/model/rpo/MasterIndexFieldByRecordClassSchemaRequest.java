package uk.gov.hmcts.darts.arm.client.model.rpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class MasterIndexFieldByRecordClassSchemaRequest {

    private String recordClassCode;
    private Boolean isForSearch;
    private Integer fieldType;
    private Boolean usePaging;

}
