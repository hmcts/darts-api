package uk.gov.hmcts.darts.cases.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.exception.CaseError;
import uk.gov.hmcts.darts.cases.helper.AdvancedSearchRequestHelper;
import uk.gov.hmcts.darts.cases.mapper.AdvancedSearchResponseMapper;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.cases.mapper.HearingEntityToCaseHearing;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.cases.util.CourtCaseUtil;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class CaseServiceImpl implements CaseService {

    private static final int MAX_RESULTS = 500;
    private final CasesMapper casesMapper;

    private final HearingRepository hearingRepository;
    private final CaseRepository caseRepository;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final AdvancedSearchRequestHelper advancedSearchRequestHelper;

    @Override
    @Transactional
    public List<ScheduledCase> getCases(GetCasesRequest request) {

        List<HearingEntity> hearings = hearingRepository.findByCourthouseCourtroomAndDate(
            request.getCourthouse(),
            request.getCourtroom(),
            request.getDate()
        );
        createCourtroomIfMissing(hearings, request);
        return casesMapper.mapToCourtCases(hearings);

    }

    @Override
    public List<Hearing> getCaseHearings(Integer caseId) {

        List<HearingEntity> hearingList = hearingRepository.findByCaseIds(List.of(caseId));

        return HearingEntityToCaseHearing.mapToHearingList(hearingList);

    }

    private void createCourtroomIfMissing(List<HearingEntity> hearings, GetCasesRequest request) {
        if (CollectionUtils.isEmpty(hearings)) {
            //find out if courthouse or courtroom are missing.
            retrieveCoreObjectService.retrieveOrCreateCourtroom(request.getCourthouse(), request.getCourtroom());
        }
        // return casesMapper.mapToCourtCases(hearings);
    }


    private ScheduledCase addCase(AddCaseRequest addCaseRequest) {
        CourtCaseEntity caseEntity = saveNewCaseEntity(addCaseRequest);
        HearingEntity savedHearingEntity;
        if (StringUtils.isNotBlank(addCaseRequest.getCourtroom())) {
            savedHearingEntity = saveNewHearingEntity(addCaseRequest, caseEntity);
            return casesMapper.mapToCourtCase(savedHearingEntity, caseEntity);
        } else {
            return casesMapper.mapToCourtCase(caseEntity);
        }
    }

    @Transactional
    @Override
    public ScheduledCase addCaseOrUpdate(AddCaseRequest addCaseRequest) {
        Optional<CourtCaseEntity> existingCase = caseRepository.findByCaseNumberAndCourthouse_CourthouseName(
            addCaseRequest.getCaseNumber(), addCaseRequest.getCourthouse());
        if (existingCase.isPresent()) {
            return updateCase(addCaseRequest, existingCase.get());
        } else {
            return addCase(addCaseRequest);
        }
    }

    private ScheduledCase updateCase(AddCaseRequest addCaseRequest, CourtCaseEntity existingCase) {
        CourtCaseEntity updatedCaseEntity = updateCaseEntity(addCaseRequest, existingCase);

        if (StringUtils.isNotBlank(addCaseRequest.getCourtroom())) {
            HearingEntity hearingEntity = updateOrCreateHearingEntity(addCaseRequest, updatedCaseEntity);
            return casesMapper.mapToCourtCase(hearingEntity, updatedCaseEntity);
        }
        return casesMapper.mapToCourtCase(updatedCaseEntity);
    }


    private HearingEntity updateOrCreateHearingEntity(AddCaseRequest addCaseRequest, CourtCaseEntity caseEntity) {

        Optional<HearingEntity> hearing = hearingRepository.findAll().stream()
            .filter(hearingEntity -> caseEntity.getId().equals(hearingEntity.getCourtCase().getId())
                && addCaseRequest.getCourtroom().equals(hearingEntity.getCourtroom().getName())
                && hearingEntity.getHearingDate().equals(LocalDate.now()))
            .findFirst();


        return hearing.map(hearingEntity -> hearingRepository.saveAndFlush(mapToHearingEntity(
            addCaseRequest,
            caseEntity,
            hearingEntity
        ))).orElseGet(() -> saveNewHearingEntity(addCaseRequest, caseEntity));
    }

    private CourtCaseEntity updateCaseEntity(AddCaseRequest addCaseRequest, CourtCaseEntity existingCase) {
        return caseRepository.saveAndFlush(casesMapper.mapAddCaseRequestToCaseEntity(addCaseRequest, existingCase));
    }

    private HearingEntity saveNewHearingEntity(AddCaseRequest addCaseRequest, CourtCaseEntity caseEntity) {
        return hearingRepository.saveAndFlush(mapToHearingEntity(addCaseRequest, caseEntity, new HearingEntity()));
    }

    private CourtCaseEntity saveNewCaseEntity(AddCaseRequest addCaseRequest) {
        CourtCaseEntity caseEntity = casesMapper.mapAddCaseRequestToCaseEntity(addCaseRequest, new CourtCaseEntity());
        return caseRepository.saveAndFlush(caseEntity);
    }

    private HearingEntity mapToHearingEntity(AddCaseRequest addCaseRequest, CourtCaseEntity caseEntity, HearingEntity hearingEntity) {
        if (!StringUtils.isBlank(addCaseRequest.getCourtroom())) {
            CourtroomEntity courtroomEntity = retrieveCoreObjectService.retrieveOrCreateCourtroom(
                addCaseRequest.getCourthouse(),
                addCaseRequest.getCourtroom()
            );
            hearingEntity.setCourtroom(courtroomEntity);
        }

        hearingEntity.setCourtCase(caseEntity);
        hearingEntity.setHearingDate(LocalDate.now());

        emptyIfNull(addCaseRequest.getJudges()).forEach(newJudge -> {
            Optional<JudgeEntity> found = emptyIfNull(hearingEntity.getJudgeList()).stream()
                .filter(j -> j.getName().equals(newJudge)).findAny();
            if (found.isEmpty()) {
                found.ifPresentOrElse(
                    j -> {
                    },
                    () -> hearingEntity.getJudgeList().add(createNewJudge(newJudge, hearingEntity))
                );
            }
        });

        return hearingEntity;
    }

    private JudgeEntity createNewJudge(String newJudge, HearingEntity hearing) {
        JudgeEntity defence = new JudgeEntity();
        defence.setHearing(hearing);
        defence.setName(newJudge);
        return defence;
    }

    @Override
    public List<AdvancedSearchResult> advancedSearch(GetCasesSearchRequest request) {
        List<CourtCaseEntity> courtCaseEntities = advancedSearchRequestHelper.getMatchingCourtCases(request);
        if (courtCaseEntities.size() > MAX_RESULTS) {
            throw new DartsApiException(CaseError.TOO_MANY_RESULTS);
        }
        if (courtCaseEntities.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> caseIds = CourtCaseUtil.getCaseIdList(courtCaseEntities);
        List<HearingEntity> hearings = hearingRepository.findByCaseIds(caseIds);
        return AdvancedSearchResponseMapper.mapResponse(hearings);
    }


}
