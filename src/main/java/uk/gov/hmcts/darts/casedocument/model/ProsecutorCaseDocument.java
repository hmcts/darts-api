package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProsecutorCaseDocument extends CreatedModifiedCaseDocument {

    private final Integer id;
    private final String name;
}
