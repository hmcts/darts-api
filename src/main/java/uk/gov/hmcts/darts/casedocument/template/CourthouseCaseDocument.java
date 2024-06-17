package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CourthouseCaseDocument {

    private final Integer id;
    private final OffsetDateTime createdDateTime;
    private final OffsetDateTime lastModifiedDateTime;
    private final Integer code;
    private final String courthouseName;
    private final RegionCaseDocument region;
    private final String displayName;
}
