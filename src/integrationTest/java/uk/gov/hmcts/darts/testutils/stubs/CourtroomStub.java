package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

@Component
@RequiredArgsConstructor
public class CourtroomStub {

    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final CourthouseStub courthouseStub;

    public CourtroomEntity createCourtroomUnlessExists(String courthouseName, String courtroomName) {
        CourthouseEntity courthouse = courthouseStub.createCourthouseUnlessExists(courthouseName);
        return retrieveCoreObjectService.retrieveOrCreateCourtroom(courthouse, courtroomName);
    }

}
