package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DefenceCaseDocument extends CreatedModifiedCaseDocument {

    private final Integer id;
    private final String name;
}
