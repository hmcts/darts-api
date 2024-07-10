package uk.gov.hmcts.darts.courthouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.TransactionalUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.RegionTestData.minimalRegion;

class CourthouseUpdateTest extends IntegrationBase {

    @Autowired
    private TransactionalUtil dbAssert;

    @Autowired
    private CourthouseService courthouseService;

    @Autowired
    private GivenBuilder given;


    @Test
    void persistsPatchedCourthouse() {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var existingRegion = dartsDatabase.save(minimalRegion());
        var courthouseEntity = someCourtHouseWithRegionAndSecurityGroup(existingRegion, 1);
        var someOtherRegionId = dartsDatabase.save(minimalRegion()).getId();

        courthouseService.updateCourthouse(courthouseEntity.getId(), new CourthousePatch()
            .regionId(someOtherRegionId)
            .courthouseName("some-new-name")
            .displayName("some-new-display-name")
            .securityGroupIds(List.of(2)));

        dbAssert.inTransaction(() -> {
            var courthouse = dartsDatabase.findCourthouseById(courthouseEntity.getId());
            assertThat(courthouse.getCourthouseName()).isEqualTo("some-new-name");
            assertThat(courthouse.getDisplayName()).isEqualTo("some-new-display-name");
            assertThat(courthouse.getRegion().getId()).isEqualTo(someOtherRegionId);
            assertThat(courthouse.getSecurityGroups()).extracting("id").containsExactlyInAnyOrder(2);
        });
    }

    private CourthouseEntity someCourtHouseWithRegionAndSecurityGroup(RegionEntity existingRegion, Integer securityGroupId) {
        var courthouseEntity = someMinimalCourthouse();
        courthouseEntity.setCourthouseName("some-name");
        courthouseEntity.setDisplayName("some-display-name");
        courthouseEntity.setRegion(existingRegion);
        courthouseEntity.setSecurityGroups(setOf(dartsDatabase.getSecurityGroupRef(securityGroupId)));
        dartsDatabase.save(courthouseEntity);
        return courthouseEntity;
    }

    private Set<SecurityGroupEntity> setOf(SecurityGroupEntity securityGroupEntity) {
        return new LinkedHashSet<>(List.of(securityGroupEntity));
    }
}
