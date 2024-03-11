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
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.courthouse.mapper.AdminCourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.model.AdminCourthouse;
import uk.gov.hmcts.darts.courthouse.model.Courthouse;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_CODE_PROVIDED_ALREADY_EXISTS;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NOT_FOUND;

@RequiredArgsConstructor
@Service
@Slf4j
public class CourthouseServiceImpl implements CourthouseService {

    private final CourthouseRepository courthouseRepository;
    private final RegionRepository regionRepository;
    private final HearingRepository hearingRepository;
    private final CaseRepository caseRepository;

    private final CourthouseToCourthouseEntityMapper courthouseMapper;

    private final AdminCourthouseToCourthouseEntityMapper adminMapper;
    private final BiValidator<CourthousePatch, Integer> courthousePatchValidator;
    private final CourthouseUpdateMapper courthouseUpdateMapper;

    @Override
    public void deleteCourthouseById(Integer id) {
        courthouseRepository.deleteById(id);
    }

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

    private void checkCourthouseIsUnique(Courthouse courthouse) {
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseNameIgnoreCase(courthouse.getCourthouseName());
        if (foundCourthouse.isPresent()) {
            throw new DartsApiException(COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS);
        }
        if (courthouse.getCode() != null) {
            foundCourthouse = courthouseRepository.findByCode(courthouse.getCode());
            if (foundCourthouse.isPresent()) {
                throw new DartsApiException(COURTHOUSE_CODE_PROVIDED_ALREADY_EXISTS);
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
