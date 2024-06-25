package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

@Component
@RequiredArgsConstructor
public class CourtroomStub {
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final CourthouseStub courthouseStub;

    public CourtroomEntity createCourtroomUnlessExists(String courthouseName, String courtroomName, UserAccountEntity userAccount) {
        CourthouseEntity courthouse = courthouseStub.createCourthouseUnlessExists(courthouseName);
        return createCourtroomUnlessExists(courthouse, courtroomName, userAccount);
    }

    public CourtroomEntity createCourtroomUnlessExists(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount) {
        return retrieveCoreObjectService.retrieveOrCreateCourtroom(courthouse, courtroomName, userAccount);
    }

}
