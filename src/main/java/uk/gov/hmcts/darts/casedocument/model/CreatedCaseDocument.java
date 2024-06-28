package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public abstract class CreatedCaseDocument {

    private OffsetDateTime createdDateTime;
    private Integer createdBy;
}
