package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;

@Component
@RequiredArgsConstructor
public class HearingStub {

    private final CourthouseStub courthouseStub;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final UserAccountStub userAccountStub;

    public HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber,
                                       LocalDateTime hearingDate) {
        courthouseStub.createCourthouseUnlessExists(courthouseName);
        return retrieveCoreObjectService.retrieveOrCreateHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate,
            userAccountStub.getSystemUserAccountEntity()
        );
    }

    public HearingEntity createHearing(CourtCaseEntity courtCase, CourtroomEntity courtroomEntity,
                                       LocalDateTime hearingDate) {
        return createHearing(courtCase.getCourthouse().getCourthouseName(), courtroomEntity.getName(), courtCase.getCaseNumber(), hearingDate);
    }

    public HearingEntity createMinimalHearing() {
        CourthouseEntity minimalCourthouse = courthouseStub.createMinimalCourthouse();
        return createHearing(minimalCourthouse.getCourthouseName(), "1", "caseNumber1", LocalDateTime.of(2020, 10, 1, 10, 0, 0));
    }

    public void createHearingsForCase(CourtCaseEntity courtCase, int numOfCourtrooms, int numOfHearingsPerCourtroom) {
        LocalDateTime startDate = LocalDateTime.of(2020, 10, 10, 10, 0, 0, 0);
        List<HearingEntity> hearings = new ArrayList<>();
        for (int courtroomCounter = 1; courtroomCounter <= numOfCourtrooms; courtroomCounter++) {
            CourtroomEntity courtroom = createCourtRoomWithNameAtCourthouse(courtCase.getCourthouse(), "courtroom" + courtroomCounter);
            for (int hearingCounter = 1; hearingCounter <= numOfHearingsPerCourtroom; hearingCounter++) {
                HearingEntity hearing = createHearing(courtCase, courtroom, startDate);
                hearings.add(hearing);
                startDate = startDate.plusDays(1);
            }
        }
        courtCase.setHearings(hearings);
    }


}
