package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CreatedModifiedCaseDocument {

    private OffsetDateTime createdDateTime;
    private Integer createdBy;
    private OffsetDateTime lastModifiedDateTime;
    private Integer lastModifiedBy;
}
