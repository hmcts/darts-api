package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

@Component
@RequiredArgsConstructor
@Deprecated
public class CourthouseStub {
    private final CourthouseStubComposable courthouseStubComposable;

    @Transactional
    public CourthouseEntity createCourthouseUnlessExists(String name) {
        return courthouseStubComposable.createCourthouseUnlessExists(name);
    }

    public CourthouseEntity createMinimalCourthouse() {
        return createCourthouseUnlessExists("minimalCourthouse");
    }

}