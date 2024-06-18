package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CourthouseStub {
    private final CourthouseRepository courthouseRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public CourthouseEntity createCourthouseUnlessExists(String name) {
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseNameIgnoreCase(name);
        return foundCourthouse.orElseGet(() -> createCourthouse(name));
    }

    private CourthouseEntity createCourthouse(String name) {
        CourthouseEntity newCourthouse = new CourthouseEntity();
        newCourthouse.setCourthouseName(name);
        newCourthouse.setDisplayName(name);
        UserAccountEntity defaultUser = userAccountRepository.getReferenceById(0);
        newCourthouse.setCreatedBy(defaultUser);
        newCourthouse.setLastModifiedBy(defaultUser);
        courthouseRepository.saveAndFlush(newCourthouse);
        return newCourthouse;
    }

    public CourthouseEntity createMinimalCourthouse() {
        return createCourthouseUnlessExists("minimalCourthouse");
    }

}
