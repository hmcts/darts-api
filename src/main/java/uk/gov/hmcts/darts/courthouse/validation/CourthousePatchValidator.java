package uk.gov.hmcts.darts.courthouse.validation;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.component.validation.BiValidator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;

import java.util.HashSet;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_DISPLAY_NAME_PROVIDED_ALREADY_EXISTS;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NAME_CANNOT_BE_CHANGED_CASES_EXISTING;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class CourthousePatchValidator implements BiValidator<CourthousePatch, Integer> {

    private final CourthouseRepository repository;
    private final CaseRepository caseRepository;
    private final RegionRepository regionRepository;
    private final SecurityGroupRepository securityGroupRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.UnnecessaryAnnotationValueElement"})
    public void validate(CourthousePatch patch, Integer id) {
        patch.setCourthouseName(StringUtils.toRootUpperCase(StringUtils.trimToEmpty(patch.getCourthouseName())));
        var courthouseEntity = repository.findById(id)
            .orElseThrow(() -> new DartsApiException(COURTHOUSE_NOT_FOUND));

        if (nonNull(patch.getCourthouseName())) {
            if (!patch.getCourthouseName().equals(courthouseEntity.getCourthouseName()) && caseRepository.existsByCourthouse(courthouseEntity)) {
                throw new DartsApiException(COURTHOUSE_NAME_CANNOT_BE_CHANGED_CASES_EXISTING);
            }
        }

        if (nonNull(patch.getCourthouseName())) {
            if (repository.existsByCourthouseNameAndIdNot(patch.getCourthouseName(), id)) {
                throw new DartsApiException(COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS);
            }
        }

        if (nonNull(patch.getDisplayName()) && repository.existsByDisplayNameIgnoreCaseAndIdNot(patch.getDisplayName(), id)) {
            throw new DartsApiException(COURTHOUSE_DISPLAY_NAME_PROVIDED_ALREADY_EXISTS);
        }

        if (nonNull(patch.getRegionId()) && !regionRepository.existsById(patch.getRegionId())) {
            throw new DartsApiException(CourthouseApiError.REGION_ID_DOES_NOT_EXIST);
        }

        if (nonNull(patch.getSecurityGroupIds())
            && !patch.getSecurityGroupIds().isEmpty()
            && !securityGroupRepository.existsAllByIdIn(new HashSet<>(patch.getSecurityGroupIds()))) {
            throw new DartsApiException(CourthouseApiError.SECURITY_GROUP_ID_DOES_NOT_EXIST);
        }
    }
}
