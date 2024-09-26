package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

@Component
@RequiredArgsConstructor
@Deprecated
public class CourtroomStub {
    private final CourtroomStubComposable courtroomStubComposable;
    private final CourthouseStubComposable courthouseStubComposable;

    public CourtroomEntity createCourtroomUnlessExists(String courthouseName, String courtroomName, UserAccountEntity userAccount) {
        CourthouseEntity courthouse = courthouseStubComposable.createCourthouseUnlessExists(courthouseName);
        return courtroomStubComposable.createCourtroomUnlessExists(courthouse, courtroomName, userAccount);
    }

}