package uk.gov.hmcts.darts.courthouse.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.courthouse.service.impl.CourthouseUpdateMapperImpl;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CourthouseEntityToAdminCourthouseMapperTest {

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private SecurityGroupRepository securityGroupRepository;
    private CourthouseUpdateMapperImpl courthouseUpdateMapper;

    @BeforeEach
    void setUp() {
        courthouseUpdateMapper = new CourthouseUpdateMapperImpl(regionRepository, securityGroupRepository);
    }

    @Test
    void mapsCourthouseEntityToAdminCourthouse() {
        var patchedCourthouse = someCourthouseWithDefaults();

        var adminCourthouse = courthouseUpdateMapper.mapEntityToAdminCourthouse(patchedCourthouse);

        assertThat(adminCourthouse.getCourthouseName()).isEqualTo("some-name");
        assertThat(adminCourthouse.getDisplayName()).isEqualTo("some-display-name");
        assertThat(adminCourthouse.getRegionId()).isEqualTo(1);
        assertThat(adminCourthouse.getSecurityGroupIds()).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(adminCourthouse.getId()).isEqualTo(1);
        assertThat(adminCourthouse.getCreatedDateTime()).isEqualTo(OffsetDateTime.parse("2020-01-01T00:00:00Z"));
        assertThat(adminCourthouse.getLastModifiedDateTime()).isEqualTo(OffsetDateTime.parse("2020-01-02T00:00:00Z"));
    }

    private static CourthouseEntity someCourthouseWithDefaults() {
        var courthouseEntity = new CourthouseEntity();
        courthouseEntity.setCourthouseName("some-name");
        courthouseEntity.setRegion(regionWithId(1));
        courthouseEntity.setSecurityGroups(someSetOfSecurityGroups());
        courthouseEntity.setDisplayName("some-display-name");
        courthouseEntity.setId(1);
        courthouseEntity.setCreatedDateTime(OffsetDateTime.parse("2020-01-01T00:00:00Z"));
        courthouseEntity.setLastModifiedDateTime(OffsetDateTime.parse("2020-01-02T00:00:00Z"));
        return courthouseEntity;
    }

    private static Set<SecurityGroupEntity> someSetOfSecurityGroups() {
        return Set.of(securityGroupWithId(1), securityGroupWithId(2), securityGroupWithId(3));
    }

    private static SecurityGroupEntity securityGroupWithId(int id) {
        var securityGroup = new SecurityGroupEntity();
        securityGroup.setId(id);
        return securityGroup;
    }

    private static RegionEntity regionWithId(int id) {
        var region = new RegionEntity();
        region.setId(id);
        return region;
    }
}
