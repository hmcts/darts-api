package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

@Component
@RequiredArgsConstructor
public class CourthouseStub {
    private final CourthouseRepository courthouseRepository;
    private final UserAccountRepository userAccountRepository;
    private final CourthouseStubComposable courthouseStubComposable;

    @Transactional
    public CourthouseEntity createCourthouseUnlessExists(String name) {
        return courthouseStubComposable.createCourthouseUnlessExists(name);
    }

    public CourthouseEntity createMinimalCourthouse() {
        return createCourthouseUnlessExists("minimalCourthouse");
    }

}