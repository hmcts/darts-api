package uk.gov.hmcts.darts.courthouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;
import uk.gov.hmcts.darts.testutils.DbAssertions;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.testutils.data.RegionTestData.minimalRegion;

class CourthouseUpdateTest extends IntegrationBase {

    @Autowired
    private DbAssertions dbAssert;

    @Autowired
    private CourthouseService courthouseService;

    @Test
    void persistsPatchedCourthouse() {
        var courthouseEntity = someMinimalCourthouseWithRegionAndSecurityGroups(minimalRegion(), 1);
        var someOtherRegion = dartsDatabase.save(minimalRegion()).getId();

        courthouseService.updateCourthouse(courthouseEntity.getId(), new CourthousePatch()
            .regionId(someOtherRegion)
            .courthouseName("some-new-name")
            .displayName("some-new-display-name")
            .securityGroupIds(List.of(2)));

        dbAssert.transactionally(() -> {
            var patchedCourthouse = dartsDatabase.findCourthouseById(courthouseEntity.getId());
            assertThat(patchedCourthouse.getCourthouseName()).isEqualTo("some-new-name");
            assertThat(patchedCourthouse.getDisplayName()).isEqualTo("some-new-display-name");
            assertThat(patchedCourthouse.getRegion().getId()).isEqualTo(someOtherRegion);
            assertThat(patchedCourthouse.getSecurityGroups()).extracting("id").containsExactlyInAnyOrder(2);
        });

    }

    private CourthouseEntity someMinimalCourthouseWithRegionAndSecurityGroups(RegionEntity region, Integer secGrp) {
        var courthouseEntity = someMinimalCourthouse();
        courthouseEntity.setRegion(dartsDatabase.save(region));
        courthouseEntity.setSecurityGroups(setOf(dartsDatabase.getSecurityGroupRef(secGrp)));
        dartsDatabase.save(courthouseEntity);
        return courthouseEntity;
    }

    private Set<SecurityGroupEntity> setOf(SecurityGroupEntity securityGroupEntity) {
        return new LinkedHashSet<>(List.of(securityGroupEntity));
    }
}
