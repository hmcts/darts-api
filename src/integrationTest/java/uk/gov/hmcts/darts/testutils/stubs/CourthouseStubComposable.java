package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Deprecated
public class CourthouseStubComposable {
    private final CourthouseRepository courthouseRepository;

    private final UserAccountRepository userAccountRepository;

    public CourthouseEntity createCourthouseUnlessExists(String name) {
        String courthouseNameUpperTrimmed = StringUtils.toRootUpperCase(StringUtils.trimToEmpty(name));
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseName(courthouseNameUpperTrimmed);
        return foundCourthouse.orElseGet(() -> createCourthouse(courthouseNameUpperTrimmed));
    }

    private CourthouseEntity createCourthouse(String name) {
        CourthouseEntity newCourthouse = new CourthouseEntity();
        newCourthouse.setCourthouseName(name);
        newCourthouse.setDisplayName(name);
        newCourthouse.setCreatedById(0);
        newCourthouse.setLastModifiedById(0);
        return courthouseRepository.saveAndFlush(newCourthouse);
    }

}