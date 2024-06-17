package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProsecutorCourtCase {

    private final Integer id;
    private final String name;
    private final OffsetDateTime lastModifiedDateTime;
    private final OffsetDateTime createdDateTime;
}
