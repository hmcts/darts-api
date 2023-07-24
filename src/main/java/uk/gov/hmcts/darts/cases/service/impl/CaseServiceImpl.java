package uk.gov.hmcts.darts.cases.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.api.CommonApi;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseServiceImpl implements CaseService {

    private final HearingRepository hearingRepository;
    private final CaseRepository caseRepository;
    private final CourtroomRepository courtroomRepository;
    private final CourthouseRepository courthouseRepository;
    private final CommonApi commonApi;

    @Override
    @Transactional
    public List<ScheduledCase> getCases(GetCasesRequest request) {

        List<HearingEntity> hearings = hearingRepository.findByCourthouseCourtroomAndDate(
            request.getCourthouse(),
            request.getCourtroom(),
            request.getDate()
        );
        createCourtroomIfMissing(hearings, request);
        return CasesMapper.mapToCourtCases(hearings);
    }


    private ScheduledCase addCase(AddCaseRequest addCaseRequest) {
        CaseEntity caseEntity = saveNewCaseEntity(addCaseRequest);
        HearingEntity savedHearingEntity;
        if (StringUtils.isNotBlank(addCaseRequest.getCourtroom())) {
            savedHearingEntity = saveNewHearingEntity(addCaseRequest, caseEntity);
            return CasesMapper.mapToCourtCase(savedHearingEntity, caseEntity);
        } else {
            return CasesMapper.mapToCourtCase(caseEntity);
        }
    }

    @Transactional
    @Override
    public ScheduledCase addCaseOrUpdate(AddCaseRequest addCaseRequest) {
        Optional<CaseEntity> existingCase = caseRepository.findByCaseNumberAndCourthouse_CourthouseName(
            addCaseRequest.getCaseNumber(), addCaseRequest.getCourthouse());
        if (existingCase.isPresent()) {
            return updateCase(addCaseRequest, existingCase.get());
        } else {
            return addCase(addCaseRequest);
        }
    }

    private ScheduledCase updateCase(AddCaseRequest addCaseRequest, CaseEntity existingCase) {
        CaseEntity updatedCaseEntity = updateCaseEntity(addCaseRequest, existingCase);

        if (StringUtils.isNotBlank(addCaseRequest.getCourtroom())) {
            HearingEntity hearingEntity = updateOrCreateHearingEntity(addCaseRequest, updatedCaseEntity);
            return CasesMapper.mapToCourtCase(hearingEntity, updatedCaseEntity);
        }
        return CasesMapper.mapToCourtCase(updatedCaseEntity);
    }


    private HearingEntity updateOrCreateHearingEntity(AddCaseRequest addCaseRequest, CaseEntity caseEntity) {

        Optional<HearingEntity> hearing = hearingRepository.findAll().stream()
            .filter(hearingEntity -> caseEntity.getId().equals(hearingEntity.getCourtCase().getId())
                && hearingEntity.getHearingDate().equals(LocalDate.now())).findFirst();


        return hearing.map(hearingEntity -> hearingRepository.saveAndFlush(mapToHearingEntity(
            addCaseRequest,
            caseEntity,
            hearingEntity
        ))).orElseGet(() -> saveNewHearingEntity(addCaseRequest, caseEntity));
    }

    private CaseEntity updateCaseEntity(AddCaseRequest addCaseRequest, CaseEntity existingCase) {
        return caseRepository.saveAndFlush(mapAddCaseRequestToCaseEntity(addCaseRequest, existingCase));
    }

    private HearingEntity saveNewHearingEntity(AddCaseRequest addCaseRequest, CaseEntity caseEntity) {
        return hearingRepository.saveAndFlush(mapToHearingEntity(addCaseRequest, caseEntity, new HearingEntity()));
    }

    private CaseEntity saveNewCaseEntity(AddCaseRequest addCaseRequest) {
        CaseEntity caseEntity = mapAddCaseRequestToCaseEntity(addCaseRequest, new CaseEntity());
        return caseRepository.saveAndFlush(caseEntity);
    }

    private HearingEntity mapToHearingEntity(AddCaseRequest addCaseRequest, CaseEntity caseEntity, HearingEntity hearingEntity) {
        if (!StringUtils.isBlank(addCaseRequest.getCourtroom())) {
            CourtroomEntity courtroomEntity = getExistingCourtroom(addCaseRequest);
            if (courtroomEntity == null) {
                throw new DartsApiException(CaseApiError.COURTROOM_PROVIDED_DOES_NOT_EXIST);
            }
            hearingEntity.setCourtroom(courtroomEntity);
        }

        hearingEntity.setCourtCase(caseEntity);
        Optional.ofNullable(addCaseRequest.getJudges())
            .ifPresentOrElse(judges -> hearingEntity.getJudges().addAll(judges), () -> hearingEntity.setJudges(null));
        return hearingEntity;
    }

    private CourtroomEntity getExistingCourtroom(AddCaseRequest addCaseRequest) {
        CourtroomEntity courtroomEntity;
        courtroomEntity = courtroomRepository.findByNames(
            addCaseRequest.getCourthouse(),
            addCaseRequest.getCourtroom()
        );
        return courtroomEntity;
    }

    private CaseEntity mapAddCaseRequestToCaseEntity(AddCaseRequest addCaseRequest, CaseEntity caseEntity) {
        caseEntity.setCaseNumber(addCaseRequest.getCaseNumber());
        Optional.ofNullable(addCaseRequest.getDefendants()).ifPresent(l -> caseEntity.getDefendants().addAll(l));
        Optional.ofNullable(addCaseRequest.getProsecutors()).ifPresent(l -> caseEntity.getProsecutors().addAll(l));
        Optional.ofNullable(addCaseRequest.getDefenders()).ifPresent(l -> caseEntity.getDefenders().addAll(l));

        Optional<CourthouseEntity> foundEntity = courthouseRepository.findByCourthouseName(addCaseRequest.getCourthouse());
        foundEntity.ifPresentOrElse(caseEntity::setCourthouse, () -> {
            throw new DartsApiException(CaseApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST);
        });

        return caseEntity;
    }

    @Override
    public List<Hearing> getCaseHearings(Integer caseId) {

        return new ArrayList<>();
    }

    private void createCourtroomIfMissing(List<HearingEntity> hearings, GetCasesRequest request) {
        if (CollectionUtils.isEmpty(hearings)) {
            //find out if courthouse or courtroom are missing.
            commonApi.retrieveOrCreateCourtroom(request.getCourthouse(), request.getCourtroom());
        }
    }


    @Override
    public List<AdvancedSearchResult> advancedSearch(GetCasesSearchRequest request) {
        return new ArrayList<>();
    }
}
