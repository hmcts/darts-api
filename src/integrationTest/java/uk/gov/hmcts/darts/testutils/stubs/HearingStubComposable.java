package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;

/**
 * Composable classes can be used as a composite to other stubs. The composable objects should be safe to inject into other stubs. This
 * will avoid circular dependencies
 */
@Component
@RequiredArgsConstructor
public class HearingStubComposable {
    private final RetrieveCoreObjectService retrieveCoreObjectService;

    public HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber,
                                       LocalDateTime hearingDate, CourthouseStub courthouseStub, UserAccountStub userAccountStub) {
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
                                       LocalDateTime hearingDate, CourthouseStub courthouseStub, UserAccountStub userAccountStub) {
        return createHearing(courtCase.getCourthouse().getCourthouseName(),
                             courtroomEntity.getName(), courtCase.getCaseNumber(), hearingDate, courthouseStub, userAccountStub);
    }

    public void createHearingsForCase(CourtCaseEntity courtCase, int numOfCourtrooms,
                                      int numOfHearingsPerCourtroom, CourthouseStub courthouseStub,  UserAccountStub userAccountStub) {
        LocalDateTime startDate = LocalDateTime.of(2020, 10, 10, 10, 0, 0, 0);
        List<HearingEntity> hearings = new ArrayList<>();
        for (int courtroomCounter = 1; courtroomCounter <= numOfCourtrooms; courtroomCounter++) {
            CourtroomEntity courtroom = createCourtRoomWithNameAtCourthouse(courtCase.getCourthouse(), "courtroom" + courtroomCounter);
            for (int hearingCounter = 1; hearingCounter <= numOfHearingsPerCourtroom; hearingCounter++) {
                HearingEntity hearing = createHearing(courtCase, courtroom, startDate, courthouseStub, userAccountStub);
                hearings.add(hearing);
                startDate = startDate.plusDays(1);
            }
        }
        courtCase.setHearings(hearings);
    }
}