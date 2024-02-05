package uk.gov.hmcts.darts.annotation.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.annotation.errors.AnnotationApiError;
import uk.gov.hmcts.darts.annotation.mapper.AnnotationMapper;
import uk.gov.hmcts.darts.annotation.service.AnnotationService;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnotationServiceImpl implements AnnotationService {

    public static final List<SecurityRoleEnum> JUDGE_AND_ADMIN_ROLES = List.of(SecurityRoleEnum.JUDGE, SecurityRoleEnum.ADMIN);

    private final CaseRepository caseRepository;

    private final AnnotationMapper annotationMapper;

    private final AuthorisationApi authorisationApi;

    private final AnnotationRepository annotationRepository;

    @Override
    public List<Annotation> getAnnotations(Integer caseId, Integer userId) {
        Optional<CourtCaseEntity> courtCaseEntity = caseRepository.findById(caseId);
        if (courtCaseEntity.isEmpty()) {
            throw new DartsApiException(AnnotationApiError.CASE_NOT_FOUND);
        }
        List<HearingEntity> hearingEntitys = courtCaseEntity.get().getHearings();

        List<Annotation> annotations = new ArrayList<>();
        if (authorisationApi.userHasOneOfRoles(List.of(SecurityRoleEnum.ADMIN))) {
            for (HearingEntity hearingEntity: hearingEntitys) {
                List<AnnotationEntity> annotationEntityList =  annotationRepository.findByHearingId(hearingEntity.getId());
                annotations.addAll(annotationEntityList
                   .stream()
                   .map(annotationEntity -> annotationMapper
                       .map(hearingEntity, annotationEntity)).toList());
            }
            return annotations;
        } else {
            for (HearingEntity hearingEntity: hearingEntitys) {
                annotations.addAll(hearingEntity.getAnnotations()
                   .stream()
                   .filter(annotationEntity -> annotationEntity.getCurrentOwner().getId().equals(userId)
                       && !annotationEntity.isDeleted())
                   .map(annotationEntity -> annotationMapper
                       .map(hearingEntity, annotationEntity)).toList());
            }
            return annotations;
        }



    }
}
