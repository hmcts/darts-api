package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchResponseItem;
import uk.gov.hmcts.darts.cases.model.CourthouseResponseObject;
import uk.gov.hmcts.darts.cases.model.CourtroomResponseObject;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class AdminCasesSearchResponseMapper {

    public List<AdminCasesSearchResponseItem> mapResponse(List<CourtCaseEntity> cases) {
        List<AdminCasesSearchResponseItem> results = new ArrayList<>();
        for (CourtCaseEntity courtCase : cases) {
            results.add(map(courtCase));
        }
        return results;
    }

    private AdminCasesSearchResponseItem map(CourtCaseEntity courtCase) {
        AdminCasesSearchResponseItem responseItem = new AdminCasesSearchResponseItem();
        responseItem.setId(courtCase.getId());
        responseItem.setCaseNumber(courtCase.getCaseNumber());
        responseItem.setCourthouse(createCourthouse(courtCase.getCourthouse()));
        responseItem.setCourtrooms(createCourtroomList(courtCase.getHearings()));
        responseItem.setJudges(courtCase.getJudgeStringList());
        responseItem.setDefendants(courtCase.getDefendantStringList());
        responseItem.isDataAnonymised(courtCase.isDataAnonymised());
        responseItem.dataAnonymisedAt(courtCase.getDataAnonymisedTs());

        return responseItem;
    }

    private CourthouseResponseObject createCourthouse(CourthouseEntity courthouse) {
        CourthouseResponseObject responseCourthouse = new CourthouseResponseObject();
        responseCourthouse.setId(courthouse.getId());
        responseCourthouse.setDisplayName(courthouse.getDisplayName());
        return responseCourthouse;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")//TODO - refactor to avoid instantiating objects in loops when this is next edited
    private List<CourtroomResponseObject> createCourtroomList(List<HearingEntity> hearings) {
        List<CourtroomEntity> courtroomEntityList = hearings.stream().map(HearingEntity::getCourtroom).distinct().toList();
        List<CourtroomResponseObject> responseList = new ArrayList<>();
        for (CourtroomEntity courtroomEntity : courtroomEntityList) {
            CourtroomResponseObject courtroomResponseObject = new CourtroomResponseObject();
            courtroomResponseObject.setId(courtroomEntity.getId());
            courtroomResponseObject.setName(courtroomEntity.getName());
            responseList.add(courtroomResponseObject);
        }
        return responseList;
    }

}
