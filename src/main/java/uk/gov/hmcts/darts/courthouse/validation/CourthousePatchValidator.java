package uk.gov.hmcts.darts.courthouse.validation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NAME_CANNOT_BE_CHANGED_CASES_EXISTING;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.COURTHOUSE_NOT_FOUND;
import static uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError.DISPLAY_NAME_PROVIDED_ALREADY_EXISTS;

@Component
@RequiredArgsConstructor
public class CourthousePatchValidator implements BiValidator<CourthousePatch, Integer> {

    private final CourthouseRepository repository;
    private final CaseRepository caseRepository;
    private final RegionRepository regionRepository;
    private final SecurityGroupRepository securityGroupRepository;

    @Override
    @Transactional(value = Transactional.TxType.REQUIRED)
    public void validate(CourthousePatch patch, Integer id) {
        var courthouseEntity = repository.findById(id)
            .orElseThrow(() -> new DartsApiException(COURTHOUSE_NOT_FOUND));

        if (nonNull(patch.getCourthouseName())) {
            if (caseRepository.existsByCourthouse(courthouseEntity)) {
                throw new DartsApiException(COURTHOUSE_NAME_CANNOT_BE_CHANGED_CASES_EXISTING);
            }

            if (repository.existsByCourthouseNameIgnoreCaseAndIdNot(patch.getCourthouseName(), id)) {
                throw new DartsApiException(COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS);
            }
        }

        if (nonNull(patch.getDisplayName())) {
            if (repository.existsByDisplayNameIgnoreCaseAndIdNot(patch.getDisplayName(), id)) {
                throw new DartsApiException(DISPLAY_NAME_PROVIDED_ALREADY_EXISTS);
            }
        }

        if (!regionRepository.existsById(patch.getRegionId())) {
            throw new DartsApiException(CourthouseApiError.REGION_DOES_NOT_EXIST);
        }


        if (!securityGroupRepository.existsAllByIdIn(new HashSet<>(patch.getSecurityGroupIds()))) {
            throw new DartsApiException(CourthouseApiError.SECURITY_GROUP_DOES_NOT_EXIST);
        }
    }
}
