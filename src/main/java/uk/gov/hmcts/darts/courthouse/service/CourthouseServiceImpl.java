package uk.gov.hmcts.darts.courthouse.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.courthouse.mapper.AdminCourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.mapper.CourthousePostToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.model.AdminCourthouse;
import uk.gov.hmcts.darts.courthouse.model.Courthouse;
import uk.gov.hmcts.darts.courthouse.model.CourthousePost;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthousePost;
import uk.gov.hmcts.darts.usermanagement.mapper.impl.SecurityGroupIdMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Slf4j
public class CourthouseServiceImpl implements CourthouseService {

    private CourthouseRepository courthouseRepository;
    private HearingRepository hearingRepository;
    private CaseRepository caseRepository;
    private RegionRepository regionRepository;
    private SecurityGroupRepository securityGroupRepository;
    private SecurityRoleRepository securityRoleRepository;
    private RetrieveCoreObjectService retrieveCoreObjectService;

    private CourthouseToCourthouseEntityMapper courthouseMapper;

    private CourthousePostToCourthouseEntityMapper courthousePostMapper;

    private final AdminCourthouseToCourthouseEntityMapper adminMapper;

    @Override
    public void deleteCourthouseById(Integer id) {
        courthouseRepository.deleteById(id);
    }

    @Override
    public CourthouseEntity amendCourthouseById(Courthouse courthouse, Integer id) {
        checkCourthouseIsUnique(courthouse);

        CourthouseEntity originalEntity = courthouseRepository.getReferenceById(id);
        originalEntity.setCourthouseName(courthouse.getCourthouseName());
        originalEntity.setCode(courthouse.getCode());

        return courthouseRepository.saveAndFlush(originalEntity);
    }

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
    public CourthouseEntity addCourtHouse(Courthouse courthouse) {
        checkCourthouseIsUnique(courthouse);
        CourthouseEntity mappedEntity = this.courthouseMapper.mapToEntity(courthouse);
        return courthouseRepository.saveAndFlush(mappedEntity);
    }

    private final SecurityGroupIdMapper securityGroupIdMapper;

    @Override
    public ExtendedCourthousePost mapFromEntitiesToExtendedCourthousePost(CourthousePost courthousePost) {
        final SecurityRoleEntity securityRoleTranscriber = getSecurityRoleByRoleName("TRANSCRIBER");
        final SecurityRoleEntity securityRoleRequester = getSecurityRoleByRoleName("REQUESTER");
        final SecurityRoleEntity securityRoleApprover = getSecurityRoleByRoleName("APPROVER");

        //check duplicate names
        checkCourthouseNameIsUnique(courthousePost);
        checkCourthouseDisplayNameIsUnique(courthousePost);

        List<Integer> securityGroupIds = (courthousePost.getSecurityGroupIds() == null) ? new ArrayList<>() : courthousePost.getSecurityGroupIds();
        courthousePost.securityGroupIds(securityGroupIds);

        //for each security_group get role_ids
        List<Integer> securityRoleIds = new ArrayList<>();
        for (Integer secGrpIds : securityGroupIds) {
            Optional<SecurityGroupEntity> securityGroupEntity = securityGroupRepository.findById(secGrpIds);
            securityGroupEntity.ifPresent(groupEntity -> securityRoleIds.add(groupEntity.getSecurityRoleEntity().getId()));
        }

        //ensure if security roles exist, TRANSCRIBER role also exists
        if (!securityRoleIds.isEmpty() && !securityRoleIds.contains(securityRoleTranscriber.getId())) {
            throw new DartsApiException(AuthorisationError.BAD_REQUEST_ONLY_TRANSCIBER_ROLES_MAY_BE_ASSIGNED);
        }

        CourthouseEntity mappedEntity = courthousePostMapper.mapToEntity(courthousePost);

        //ensure region exists in dB
        List<RegionEntity> regions = regionRepository.findAll();
        if (CollectionUtils.isNotEmpty(regions) && courthousePost.getRegionId() != null) {
            List<Integer> regionIds = regions.stream().map(RegionEntity::getId).toList();

            if (CollectionUtils.isNotEmpty(regionIds) && !regionIds.contains(courthousePost.getRegionId())) {
                throw new DartsApiException(AuthorisationError.BAD_REQUEST_REGION_ID_DOES_NOT_EXIST);
            }

            //get region
            RegionEntity regionFound = null;
            for (RegionEntity region : regions) {
                if (region.getId() == courthousePost.getRegionId()) {
                    regionFound = region;
                }
            }

            mappedEntity.setRegion(regionFound);
        }

        Set<SecurityGroupEntity> securityGroupEntities =
            (CollectionUtils.isEmpty(mappedEntity.getSecurityGroups())) ? new LinkedHashSet<>() : mappedEntity.getSecurityGroups();

        for (Integer id : securityRoleIds) {
            SecurityGroupEntity securityGroupEntity = addSecurityGroupForCourthouse(courthousePost, getSecurityRoleByRoleId(id));
            securityGroupEntities.add(securityGroupEntity);
        }
        SecurityGroupEntity securityGroupRequesterEntity = addSecurityGroupForCourthouse(courthousePost, securityRoleRequester);
        SecurityGroupEntity securityGroupApprovarEntity = addSecurityGroupForCourthouse(courthousePost, securityRoleApprover);

        securityGroupEntities.add(securityGroupApprovarEntity);
        securityGroupEntities.add(securityGroupRequesterEntity);
        mappedEntity.setSecurityGroups(securityGroupEntities);

        courthouseRepository.saveAndFlush(mappedEntity);

        ExtendedCourthousePost extendedCourthousePost = courthousePostMapper.mapFromEntityToExtendedCourthousePost(courthousePost);

        extendedCourthousePost.setId(mappedEntity.getId());
        extendedCourthousePost.setCreatedDateTime(mappedEntity.getCreatedDateTime());
        extendedCourthousePost.setLastModifiedDateTime(mappedEntity.getLastModifiedDateTime());
        extendedCourthousePost.securityGroupIds(securityGroupIdMapper.mapSecurityGroupEntitiesToIds(mappedEntity.getSecurityGroups()));
        extendedCourthousePost.setRegionId(mappedEntity.getRegion() == null ? null : mappedEntity.getRegion().getId());

        return extendedCourthousePost;
    }

    private SecurityGroupEntity addSecurityGroupForCourthouse(CourthousePost courthousePost, SecurityRoleEntity securityRole) {
        SecurityGroupEntity securityGroupEntity = new SecurityGroupEntity();

        securityGroupEntity.setDisplayName(courthousePost.getCourthouseName());
        securityGroupEntity.setGroupName(courthousePost.getCourthouseName());
        securityGroupEntity.setGlobalAccess(false);
        securityGroupEntity.setDisplayState(true);
        securityGroupEntity.setUseInterpreter(false);
        securityGroupEntity.setSecurityRoleEntity(securityRole);
        securityGroupRepository.saveAndFlush(securityGroupEntity);

        return securityGroupEntity;
    }

    private SecurityRoleEntity getSecurityRoleByRoleName(String scurityRole) {
        assert securityRoleRepository != null;
        List<SecurityRoleEntity> securityRoleEntities = securityRoleRepository.findAllByOrderById();
        for (SecurityRoleEntity securityRoleEntity: securityRoleEntities) {
            if (securityRoleEntity.getRoleName().equals(scurityRole)) {
                return securityRoleEntity;
            }
        }
        return null;
    }

    private SecurityRoleEntity getSecurityRoleByRoleId(Integer roleId) {
        List<SecurityRoleEntity> securityRoleEntities = securityRoleRepository.findAllByOrderById();
        for (SecurityRoleEntity securityRoleEntity: securityRoleEntities) {
            if (securityRoleEntity.getId().equals(roleId)) {
                return securityRoleEntity;
            }
        }
        return null;
    }

    private void checkCourthouseDisplayNameIsUnique(CourthousePost courthousePost) {
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseNameIgnoreCase(courthousePost.getDisplayName());
        if (foundCourthouse.isPresent()) {
            throw new DartsApiException(CourthouseApiError.COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS);
        }
    }

    private void checkCourthouseNameIsUnique(CourthousePost courthousePost) {
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseNameIgnoreCase(courthousePost.getCourthouseName());
        if (foundCourthouse.isPresent()) {
            throw new DartsApiException(CourthouseApiError.COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS);
        }
    }


    private void checkCourthouseIsUnique(Courthouse courthouse) {
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseNameIgnoreCase(courthouse.getCourthouseName());
        if (foundCourthouse.isPresent()) {
            throw new DartsApiException(CourthouseApiError.COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS);
        }
        if (courthouse.getCode() != null) {
            foundCourthouse = courthouseRepository.findByCode(courthouse.getCode());
            if (foundCourthouse.isPresent()) {
                throw new DartsApiException(CourthouseApiError.COURTHOUSE_CODE_PROVIDED_ALREADY_EXISTS);
            }

        }
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
}
