package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.LocalDateTime;

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

    public HearingEntity createHearingWithMedia(String courthouseName, String courtroomName, String caseNumber,
                                       LocalDateTime hearingDate, MediaEntity mediaEntity) {
        courthouseStub.createCourthouseUnlessExists(courthouseName);
        return retrieveCoreObjectService.retrieveOrCreateHearingWithMedia(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate,
            userAccountStub.getSystemUserAccountEntity(),
            mediaEntity
        );
    }

    public HearingEntity createMinimalHearing() {
        CourthouseEntity minimalCourthouse = courthouseStub.createMinimalCourthouse();
        return createHearing(minimalCourthouse.getCourthouseName(), "1", "caseNumber1", LocalDateTime.of(2020, 10, 1, 10, 0, 0));
    }
}