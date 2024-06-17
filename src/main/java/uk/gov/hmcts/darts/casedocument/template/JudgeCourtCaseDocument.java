package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JudgeCourtCaseDocument extends CreatedModifiedCaseDocument {

    private Integer id;
    private String name;
}
