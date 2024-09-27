package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Deprecated
public class CourthouseStubComposable {
    @Autowired
    private CourthouseRepository courthouseRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    public CourthouseEntity createCourthouseUnlessExists(String name) {
        String nameUC = StringUtils.toRootUpperCase(name);
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseName(nameUC);
        return foundCourthouse.orElseGet(() -> createCourthouse(nameUC));
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

}