package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CaseLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class LinkedCaseMapper {

    public List<CourtCaseEntity> mapLinkedCases(CourtCaseEntity courtCase, List<CaseLinkedCaseEntity> linkedCaseEntities) {
        List<CourtCaseEntity> linkedCases = new ArrayList<>();
        for (CaseLinkedCaseEntity linkedCaseEntity : linkedCaseEntities) {
            CourtCaseEntity linkedCase = getLinkedCase(courtCase, linkedCaseEntity);
            if (linkedCase != null) {
                linkedCases.add(linkedCase);
            }
        }
        return linkedCases;
    }

    private CourtCaseEntity getLinkedCase(CourtCaseEntity courtCase, CaseLinkedCaseEntity linkedCaseEntity) {
        CourtCaseEntity courtCase1 = linkedCaseEntity.getCourtCase1();
        if (isSameCase(courtCase, courtCase1)) {
            return linkedCaseEntity.getCourtCase2();
        }

        CourtCaseEntity courtCase2 = linkedCaseEntity.getCourtCase2();
        if (isSameCase(courtCase, courtCase2)) {
            return courtCase1;
        }

        return null;
    }

    private boolean isSameCase(CourtCaseEntity courtCase, CourtCaseEntity linkedCase) {
        return courtCase != null
            && linkedCase != null
            && courtCase.getId().equals(linkedCase.getId());
    }
}
