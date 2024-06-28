package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CourthouseCaseDocument extends CreatedModifiedCaseDocument {

    private final Integer id;
    private final Integer code;
    private final String courthouseName;
    private final RegionCaseDocument region;
    private final String displayName;
}
