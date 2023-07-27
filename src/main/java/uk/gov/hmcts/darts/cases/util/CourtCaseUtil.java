package uk.gov.hmcts.darts.cases.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;

import java.util.List;

@UtilityClass
public class CourtCaseUtil {

    public List<Integer> getCaseIdList(List<CourtCaseEntity> courtCaseEntities) {
        return CollectionUtils.emptyIfNull(courtCaseEntities).stream().map(CourtCaseEntity::getId).toList();
    }
}
