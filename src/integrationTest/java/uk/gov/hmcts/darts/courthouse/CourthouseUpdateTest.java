package uk.gov.hmcts.darts.courthouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.TransactionalAssert;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.testutils.data.RegionTestData.minimalRegion;

class CourthouseUpdateTest extends IntegrationBase {

    @Autowired
    private TransactionalAssert dbAssert;

    @Autowired
    private CourthouseService courthouseService;


    @Test
    void persistsPatchedCourthouse() {
        var existingRegion = dartsDatabase.save(minimalRegion());
        var courthouseEntity = someMinimalCourthouse();
        courthouseEntity.setCourthouseName("some-name");
        courthouseEntity.setDisplayName("some-display-name");
        courthouseEntity.setRegion(existingRegion);
        courthouseEntity.setSecurityGroups(setOf(dartsDatabase.getSecurityGroupRef(1)));
        dartsDatabase.save(courthouseEntity);
        var updatedRegionId = dartsDatabase.save(minimalRegion()).getId();

        courthouseService.updateCourthouse(courthouseEntity.getId(), new CourthousePatch()
            .regionId(updatedRegionId)
            .courthouseName("some-new-name")
            .displayName("some-new-display-name")
            .securityGroupIds(List.of(2)));

        dbAssert.assertWithTransaction(() -> {
            assertThat(dartsDatabase.findCourthouseById(courthouseEntity.getId()))
                .extracting("courthouseName").isEqualTo("some-new-name")
                .extracting("displayName").isEqualTo("some-new-display-name")
                .extracting("regions").isEqualTo(updatedRegionId);
        });

    }

    private Set<SecurityGroupEntity> setOf(SecurityGroupEntity securityGroupEntity) {
        return new LinkedHashSet<>(List.of(securityGroupEntity));
    }


    private String regionPatch(Integer regionId) {
         return String.format("""
            {
              "region_id": %d
            }""", regionId);
    }

    private String courthouseNamePatch(String name) {
        return String.format("""
            {
              "courthouse_name": "%s"
            }""", name);
    }

    private String displayNamePatch(String displayName) {
        return String.format("""
            {
              "display_name": "%s"
            }""", displayName);
    }

    private String securityGroupsPatch(String grpIds) {
        return String.format("""
            {
              "security_group_ids": [ %s ]
            }""", grpIds);
    }
}
