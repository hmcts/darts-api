package uk.gov.hmcts.darts.common.model.retrievecoreobjectservice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

@Getter
@Setter
@AllArgsConstructor
public class CaseIdAndCourthouse {
    CourthouseEntity courthouse;
    Integer caseId;
}
