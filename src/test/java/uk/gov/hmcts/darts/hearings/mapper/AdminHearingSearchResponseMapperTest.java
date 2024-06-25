package uk.gov.hmcts.darts.hearings.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchResponse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminHearingSearchResponseMapperTest {

    @Test
    void mapSearchResponse() {
        List<HearingEntity> hearingEntityList = new ArrayList<>();
        hearingEntityList.add(setupHearing(1));
        hearingEntityList.add(setupHearing(2));
        hearingEntityList.add(setupHearing(3));
        List<HearingsSearchResponse> actualResponse = AdminHearingSearchResponseMapper.mapResponse(hearingEntityList);

        for (int i = 0; i < hearingEntityList.size(); i++) {
            assertEquals(hearingEntityList.get(i).getHearingDate(), actualResponse.get(i).getHearingDate());
            assertEquals(hearingEntityList.get(i).getId(), actualResponse.get(i).getId());
            assertEquals(hearingEntityList.get(i).getCourtroom().getCourthouse().getId(), actualResponse.get(i).getCourthouse().getId());
            assertEquals(hearingEntityList.get(i).getCourtroom().getCourthouse().getDisplayName(), actualResponse.get(i).getCourthouse().getDisplayName());
            assertEquals(hearingEntityList.get(i).getCourtroom().getId(), actualResponse.get(i).getCourtroom().getId());
            assertEquals(hearingEntityList.get(i).getCourtroom().getName(), actualResponse.get(i).getCourtroom().getName());
            assertEquals(hearingEntityList.get(i).getCourtCase().getId(), actualResponse.get(i).getCase().getId());
            assertEquals(hearingEntityList.get(i).getCourtCase().getCaseNumber(), actualResponse.get(i).getCase().getCaseNumber());
        }
    }


    public static  HearingEntity setupHearing(Integer id) {
        return setupHearing(id, id, id, id);
    }

    public static HearingEntity setupHearing(Integer id, Integer courthouseId, Integer courtroomId, Integer courtcaseId) {

        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setId(courthouseId);
        courthouseEntity.setDisplayName("Display name " + courthouseId);

        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setId(courtroomId);
        courtroomEntity.setName("Name " + courtcaseId);

        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(courtcaseId);
        courtCaseEntity.setCaseNumber("Case number" + courtcaseId);

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setId(id);
        hearingEntity.setHearingDate(LocalDate.now());
        hearingEntity.setCourtCase(courtCaseEntity);
        hearingEntity.setCourtroom(courtroomEntity);

        courtroomEntity.setCourthouse(courthouseEntity);

        return hearingEntity;
    }
}