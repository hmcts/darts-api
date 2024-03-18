package uk.gov.hmcts.darts.courthouse.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.component.validation.BiValidator;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.model.SecurityGroupModel;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.courthouse.mapper.AdminCourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.model.AdminCourthouse;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.courthouse.model.CourthousePost;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthousePost;
import uk.gov.hmcts.darts.usermanagement.api.UserManagementApi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NOT_FOUND;

@RequiredArgsConstructor
@Service
@Slf4j
public class CourthouseServiceImpl implements CourthouseService {

    private final CourthouseRepository courthouseRepository;
    private final HearingRepository hearingRepository;
    private final CaseRepository caseRepository;
    private final RegionRepository regionRepository;
    private final SecurityGroupRepository securityGroupRepository;
    private final SecurityRoleRepository securityRoleRepository;

    private final AdminCourthouseToCourthouseEntityMapper adminMapper;
    private final CourthouseToCourthouseEntityMapper courthouseMapper;
    private final CourthouseUpdateMapper courthouseUpdateMapper;

    private final UserManagementApi userManagementApi;

    private final BiValidator<CourthousePatch, Integer> courthousePatchValidator;

    private final uk.gov.hmcts.darts.common.util.StringUtils stringUtils;

    // TODO: needs to be removed. Only used in test
    @Override
    public CourthouseEntity getCourtHouseById(Integer id) {
        return courthouseRepository.getReferenceById(id);
    }

    @Override
    public AdminCourthouse getAdminCourtHouseById(Integer id) {
        CourthouseEntity courthouseEntity = courthouseRepository.getReferenceById(id);

        Set<SecurityGroupEntity> secGrps = courthouseEntity.getSecurityGroups();
        List<Integer> secGrpIds = new ArrayList<>();
        if (secGrps != null && !secGrps.isEmpty()) {
            secGrpIds = secGrps.stream().map(SecurityGroupEntity::getId).collect(Collectors.toList());
        }

        AdminCourthouse adminCourthouse = adminMapper.mapFromEntityToAdminCourthouse(courthouseEntity);
        adminCourthouse.setSecurityGroupIds(secGrpIds);

        RegionEntity region = courthouseEntity.getRegion();

        if (region != null) {
            adminCourthouse.setRegionId(region.getId());
        }

        adminCourthouse.setHasData(hearingRepository.hearingsExistForCourthouse(id)
            || caseRepository.caseExistsForCourthouse(id));

        return adminCourthouse;
    }

    @Override
    public List<RegionEntity> getAdminAllRegions() {
        return regionRepository.findAll();
    }

    @Override
    public List<ExtendedCourthouse> mapFromEntitiesToExtendedCourthouses(List<CourthouseEntity> courthouseEntities) {
        List<ExtendedCourthouse> extendedCourthouses = new ArrayList<>();

        courthouseEntities
            .forEach(courthouseEntity -> {
                ExtendedCourthouse extendedCourthouse = courthouseMapper.mapFromEntityToExtendedCourthouse(courthouseEntity);
                extendedCourthouse.setRegionId(courthouseEntity.getRegion() == null ? null : courthouseEntity.getRegion().getId());
                extendedCourthouses.add(extendedCourthouse);
            });

        return extendedCourthouses;
    }

    @Override
    public List<CourthouseEntity> getAllCourthouses() {
        return courthouseRepository.findAll();
    }

    @Override
    @Transactional
    public ExtendedCourthousePost createCourthouseAndGroups(CourthousePost courthousePost) {
        checkCourthouseNameIsUnique(courthousePost.getCourthouseName());
        checkCourthouseDisplayNameIsUnique(courthousePost.getDisplayName());
        RegionEntity validatedRegionEntity = checkRegionExists(courthousePost.getRegionId());
        Set<SecurityGroupEntity> validatedSecurityGroupEntities = checkSecurityGroupsExistAndArePermitted(courthousePost.getSecurityGroupIds());

        String courthouseName = courthousePost.getCourthouseName();
        String displayName = courthousePost.getDisplayName();
        validatedSecurityGroupEntities.add(createAndSaveGroupForRole(courthouseName, displayName, SecurityRoleEnum.APPROVER));
        validatedSecurityGroupEntities.add(createAndSaveGroupForRole(courthouseName, displayName, SecurityRoleEnum.REQUESTER));

        var courthouseEntity = createAndSaveCourthouseEntity(courthousePost, validatedRegionEntity, validatedSecurityGroupEntities);

        return mapToPostResponse(courthouseEntity);
    }

    private ExtendedCourthousePost mapToPostResponse(CourthouseEntity courthouseEntity) {
        ExtendedCourthousePost courthouse = courthouseMapper.mapToExtendedCourthousePost(courthouseEntity);

        courthouse.securityGroupIds(courthouseEntity.getSecurityGroups().stream()
                                        .map(SecurityGroupEntity::getId)
                                        .toList());
        courthouse.setRegionId(courthouseEntity.getRegion() == null ? null : courthouseEntity.getRegion().getId());

        return courthouse;
    }

    private CourthouseEntity createAndSaveCourthouseEntity(CourthousePost courthousePost,
                                                           RegionEntity regionEntity,
                                                           Set<SecurityGroupEntity> securityGroupEntities) {
        CourthouseEntity courthouseEntity = courthouseMapper.mapToEntity(courthousePost);

        courthouseEntity.setRegion(regionEntity);
        courthouseEntity.setSecurityGroups(securityGroupEntities);

        courthouseRepository.saveAndFlush(courthouseEntity);

        return courthouseEntity;
    }

    private SecurityGroupEntity createAndSaveGroupForRole(String courthouseName,
                                                          String displayName,
                                                          SecurityRoleEnum securityRole) {
        var securityRoleEntity = securityRoleRepository.findByRoleName(securityRole.name())
            .orElseThrow();

        SecurityGroupModel securityGroupModel = SecurityGroupModel.builder()
            .name(stringUtils.toScreamingSnakeCase(courthouseName + " " + securityRoleEntity.getRoleName()))
            .displayName(displayName + " " + securityRoleEntity.getDisplayName())
            .description("System generated group for " + courthouseName)
            .useInterpreter(false)
            .roleId(securityRoleEntity.getId())
            .build();

        return userManagementApi.createAndSaveSecurityGroup(securityGroupModel);
    }

    private void checkCourthouseDisplayNameIsUnique(String courthouseDisplayName) {
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByDisplayNameIgnoreCase(courthouseDisplayName);

        if (foundCourthouse.isPresent()) {
            throw new DartsApiException(CourthouseApiError.COURTHOUSE_DISPLAY_NAME_PROVIDED_ALREADY_EXISTS);
        }
    }

    private void checkCourthouseNameIsUnique(String courthouseName) {
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseNameIgnoreCase(courthouseName);

        if (foundCourthouse.isPresent()) {
            throw new DartsApiException(CourthouseApiError.COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS);
        }
    }

    private RegionEntity checkRegionExists(Integer regionId) {
        if (regionId == null) {
            return null;
        }

        Optional<RegionEntity> regionOptional = regionRepository.findById(regionId);
        if (regionOptional.isEmpty()) {
            throw new DartsApiException(CourthouseApiError.REGION_ID_DOES_NOT_EXIST);
        }
        return regionOptional.get();
    }

    private Set<SecurityGroupEntity> checkSecurityGroupsExistAndArePermitted(List<Integer> securityGroupIds) {
        if (securityGroupIds == null) {
            return new HashSet<>();
        }

        Set<SecurityGroupEntity> validatedSecurityGroupEntities = new HashSet<>();
        for (Integer securityGroupId : securityGroupIds) {
            SecurityGroupEntity securityGroupEntity = securityGroupRepository.findById(securityGroupId)
                .orElseThrow(() -> new DartsApiException(CourthouseApiError.SECURITY_GROUP_ID_DOES_NOT_EXIST));
            validatedSecurityGroupEntities.add(securityGroupEntity);
        }

        boolean isAnyDisallowedRolePresent = validatedSecurityGroupEntities.stream()
            .map(SecurityGroupEntity::getSecurityRoleEntity)
            .map(SecurityRoleEntity::getRoleName)
            .anyMatch(roleName -> !roleName.equals(SecurityRoleEnum.TRANSCRIBER.name()));

        if (isAnyDisallowedRolePresent) {
            throw new DartsApiException(CourthouseApiError.ONLY_TRANSCRIBER_ROLES_MAY_BE_ASSIGNED);
        }

        return validatedSecurityGroupEntities;
    }

    /**
     * retrieves the courtroom from the database. If the database doesn't have the code, then it will insert it.
     *
     * @param courthouseCode Optional parameter. If it is not provided, then name will be used by itself.
     * @param courthouseName Name of the courthouse to search for.
     * @return the found courtroom
     * @throws CourthouseNameNotFoundException when the courthouse isn't found
     * @throws CourthouseCodeNotMatchException when the courtroom is found, but it has a different code that expected.
     */
    @Override
    @SuppressWarnings("PMD.UselessParentheses")
    public CourthouseEntity retrieveAndUpdateCourtHouse(Integer courthouseCode, String courthouseName)
        throws CourthouseNameNotFoundException, CourthouseCodeNotMatchException {
        CourthouseEntity foundCourthouse = retrieveCourthouse(courthouseCode, courthouseName);
        if (foundCourthouse.getCode() == null && courthouseCode != null) {
            //update courthouse in database with new code
            foundCourthouse.setCode(courthouseCode);
            courthouseRepository.saveAndFlush(foundCourthouse);
        } else {
            if (!StringUtils.equalsIgnoreCase(foundCourthouse.getCourthouseName(), courthouseName)
                || (courthouseCode != null && !Objects.equals(courthouseCode, foundCourthouse.getCode()))) {
                throw new CourthouseCodeNotMatchException(foundCourthouse, courthouseCode, courthouseName);
            }
        }
        return foundCourthouse;
    }

    private CourthouseEntity retrieveCourthouse(Integer courthouseCode, String courthouseName) throws CourthouseNameNotFoundException {
        String courthouseNameUC = StringUtils.upperCase(courthouseName);
        Optional<CourthouseEntity> courthouseOptional = Optional.empty();
        if (courthouseCode != null) {
            courthouseOptional = courthouseRepository.findByCode(courthouseCode.shortValue());
        }
        if (courthouseOptional.isEmpty()) {
            //code not found, lookup name instead
            courthouseOptional = courthouseRepository.findByCourthouseNameIgnoreCase(courthouseNameUC);
            if (courthouseOptional.isEmpty()) {
                throw new CourthouseNameNotFoundException(courthouseNameUC);
            }
        }
        return courthouseOptional.get();
    }

    @Override
    @Transactional
    public AdminCourthouse updateCourthouse(Integer courthouseId, CourthousePatch courthousePatch) {
        var courthouseEntity = courthouseRepository.findById(courthouseId)
            .orElseThrow(() -> new DartsApiException(COURTHOUSE_NOT_FOUND));
        courthousePatchValidator.validate(courthousePatch, courthouseId);

        var patchedCourthouse = courthouseUpdateMapper.mapPatchToEntity(courthousePatch, courthouseEntity);
        courthouseRepository.save(patchedCourthouse);

        return courthouseUpdateMapper.mapEntityToAdminCourthouse(patchedCourthouse);
    }

}
