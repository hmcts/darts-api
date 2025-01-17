package uk.gov.hmcts.darts.courthouse.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.courthouse.model.AdminCourthouse;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.courthouse.service.CourthouseUpdateMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;

@RequiredArgsConstructor
@Component
@Slf4j
public class CourthouseUpdateMapperImpl implements CourthouseUpdateMapper {

    private final RegionRepository regionRepository;
    private final SecurityGroupRepository securityGroupRepository;

    @Transactional
    @Override
    public CourthouseEntity mapPatchToEntity(CourthousePatch courthousePatch, CourthouseEntity courthouseEntity) {
        if (nonNull(courthousePatch.getCourthouseName())) {
            courthouseEntity.setCourthouseName(courthousePatch.getCourthouseName());
        }

        if (nonNull(courthousePatch.getDisplayName())) {
            courthouseEntity.setDisplayName(courthousePatch.getDisplayName());
        }

        if (nonNull(courthousePatch.getRegionId())) {
            var regionReference = regionRepository.getReferenceById(courthousePatch.getRegionId());
            courthouseEntity.setRegion(regionReference);
        }

        if (nonNull(courthousePatch.getSecurityGroupIds())) {
            courthouseEntity.setSecurityGroups(securityGroupsFrom(courthousePatch));
        }

        return courthouseEntity;
    }

    @Override
    public AdminCourthouse mapEntityToAdminCourthouse(CourthouseEntity patchedCourthouse) {
        var adminCourthouse = new AdminCourthouse();

        adminCourthouse.setCode(patchedCourthouse.getCode());
        adminCourthouse.setDisplayName(patchedCourthouse.getDisplayName());
        adminCourthouse.setCourthouseName(patchedCourthouse.getCourthouseName());
        adminCourthouse.setSecurityGroupIds(groupIdsFrom(patchedCourthouse));
        adminCourthouse.setId(patchedCourthouse.getId());
        Optional.ofNullable(patchedCourthouse.getRegion()).ifPresent(
            region -> adminCourthouse.setRegionId(region.getId())
        );
        adminCourthouse.setLastModifiedDateTime(patchedCourthouse.getLastModifiedDateTime());
        adminCourthouse.setCreatedDateTime(patchedCourthouse.getCreatedDateTime());

        return adminCourthouse;
    }

    private Set<SecurityGroupEntity> securityGroupsFrom(CourthousePatch courthousePatch) {
        return courthousePatch.getSecurityGroupIds().stream()
            .map(securityGroupRepository::getReferenceById)
            .collect(toSet());
    }

    private static List<Integer> groupIdsFrom(CourthouseEntity patchedCourthouse) {
        return patchedCourthouse.getSecurityGroups().stream()
            .map(SecurityGroupEntity::getId)
            .toList();
    }
}
