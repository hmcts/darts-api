package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CourthouseStub {

    private final CourthouseRepository courthouseRepository;

    @Transactional
    public CourthouseEntity createCourthouseUnlessExists(String name) {
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseNameIgnoreCase(name);
        return foundCourthouse.orElseGet(() -> createCourthouse(name));
    }

    private CourthouseEntity createCourthouse(String name) {
        CourthouseEntity newCourthouse = new CourthouseEntity();
        newCourthouse.setCourthouseName(name);
        newCourthouse.setDisplayName(name);
        courthouseRepository.saveAndFlush(newCourthouse);
        return newCourthouse;
    }

    public CourthouseEntity createMinimalCourthouse() {
        return createCourthouseUnlessExists("minimalCourthouse");
    }

}
