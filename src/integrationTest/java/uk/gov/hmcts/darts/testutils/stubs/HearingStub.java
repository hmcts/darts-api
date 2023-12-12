package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class HearingStub {

    private final CourthouseStub courthouseStub;
    private final RetrieveCoreObjectService retrieveCoreObjectService;


    public HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber,
                                       LocalDate hearingDate) {
        courthouseStub.createCourthouseUnlessExists(courthouseName);
        return retrieveCoreObjectService.retrieveOrCreateHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate
        );
    }
}
