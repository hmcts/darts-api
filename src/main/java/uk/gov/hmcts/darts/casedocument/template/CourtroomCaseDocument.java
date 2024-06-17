package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CourtroomCaseDocument extends CreatedCaseDocument {

    private final Integer id;
    private final String name;
}
