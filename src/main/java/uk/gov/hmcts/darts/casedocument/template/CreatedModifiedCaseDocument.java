package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public abstract class CreatedModifiedCaseDocument {

    private OffsetDateTime createdDateTime;
    private Integer createdBy;
    private OffsetDateTime lastModifiedDateTime;
    private Integer lastModifiedBy;
}
