package uk.gov.hmcts.darts.arm.model.rpo;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MasterIndexFieldByRecordClassSchema {

    private String masterIndexField;
    private String displayName;
    private String propertyName;
    private String propertyType;
    private Boolean isMasked;

}
