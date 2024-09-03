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
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.courthouse.service.impl.CourthouseUpdateMapperImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourthouseUpdatePatchToEntityMapperTest {

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
    void mapsCourthouseNameOnlyPatch() {
        var courthouseEntity = courthouseUpdateMapper.mapPatchToEntity(
            new CourthousePatch().courthouseName("some-name"), new CourthouseEntity());

        assertThat(courthouseEntity.getCourthouseName()).isEqualTo("SOME-NAME");
    }

    @Test
    void mapsDisplayNameOnlyPatch() {
        var courthouseEntity = courthouseUpdateMapper.mapPatchToEntity(
            new CourthousePatch().displayName("some-display-name"), new CourthouseEntity());

        assertThat(courthouseEntity.getDisplayName()).isEqualTo("some-display-name");
    }

    @Test
    void mapsRegionOnlyPatch() {
        when(regionRepository.getReferenceById(1)).thenReturn(regionWithId(1));

        var courthouseEntity = courthouseUpdateMapper.mapPatchToEntity(
            new CourthousePatch().regionId(1), new CourthouseEntity());

        assertThat(courthouseEntity.getRegion())
            .extracting("id").isEqualTo(1);
    }

    @Test
    void mapsSecurityGroupsOnlyPatch() {
        when(securityGroupRepository.getReferenceById(1)).thenReturn(securityGroupWithId(1));
        when(securityGroupRepository.getReferenceById(2)).thenReturn(securityGroupWithId(2));
        when(securityGroupRepository.getReferenceById(3)).thenReturn(securityGroupWithId(3));

        var courthouseEntity = courthouseUpdateMapper.mapPatchToEntity(
            new CourthousePatch().securityGroupIds(List.of(1, 2, 3)), new CourthouseEntity());

        assertThat(courthouseEntity.getSecurityGroups())
            .extracting("id").containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void mapsFullPatch() {
        var fullCourthousePatch = new CourthousePatch()
            .displayName("some-display-name")
            .courthouseName("some-name")
            .regionId(1)
            .securityGroupIds(List.of(1));
        when(regionRepository.getReferenceById(fullCourthousePatch.getRegionId())).thenReturn(regionWithId(1));
        when(securityGroupRepository.getReferenceById(1)).thenReturn(securityGroupWithId(1));

        var courthouseEntity = courthouseUpdateMapper.mapPatchToEntity(fullCourthousePatch, new CourthouseEntity());

        assertThat(courthouseEntity.getCourthouseName()).isEqualTo("SOME-NAME");
        assertThat(courthouseEntity.getDisplayName()).isEqualTo("some-display-name");
        assertThat(courthouseEntity.getSecurityGroups())
            .extracting("id").containsExactlyInAnyOrder(1);
        assertThat(courthouseEntity.getRegion())
            .extracting("id").isEqualTo(1);
    }

    private SecurityGroupEntity securityGroupWithId(int id) {
        var securityGroup = new SecurityGroupEntity();
        securityGroup.setId(id);
        securityGroup.setDisplayState(true);
        return securityGroup;
    }

    private static RegionEntity regionWithId(int id) {
        var region = new RegionEntity();
        region.setId(id);
        return region;
    }
}
